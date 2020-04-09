(ns com.github.hindol.twenty-nine.events
  (:require
   [clojure.core.async :as async]
   [com.github.hindol.twenty-nine.db :as db]
   [com.github.hindol.twenty-nine.engine :as engine]
   [com.github.hindol.twenty-nine.ws :as ws]
   [editscript.core :as edit]
   [editscript.edit :as fmt]))

(def event-handlers (atom {}))

(let [event-queue (async/chan)
      event-loop  (atom nil)]
  (defn dispatch
    [event]
    (async/go
      (async/>! event-queue event))
    (when-not @event-loop
      (reset! event-loop
              (async/go-loop []
                (when-let [[dequeued-id :as dequeued] (async/<! event-queue)]
                  (if-let [handler (get @event-handlers dequeued-id)]
                    (swap! db/app-db handler dequeued)
                    (println "No handler registered for event: " (pr-str dequeued-id)))
                  (recur)))))))

(defn on-event
  [id handler]
  (swap! event-handlers assoc id handler))

(on-event
 :init-game
 (fn [_ _]
   (let [game (db/game)]
     (ws/broadcast! [:init-db game])
     game)))

(on-event
 :change-turn
 (fn [db _]
   (loop []
     (let [round                            (get-in db [:rounds :current])
           hands                            (:hands round)
           {:keys [turns plays]
            :as   trick} (get-in round [:tricks :current])
           turn                             (get turns (count plays) :end-trick)]
       (case turn
         :end-trick (do
                      (dispatch [:end-trick])
                      db)
         (case (get (:players db) turn)
           :machine (let [card   (engine/play (get hands turn) trick)
                          new-db (-> db
                                     (assoc-in [:rounds :current :tricks :current :plays turn] card)
                                     (update-in [:rounds :current :hands turn] #(filterv (complement #{card}) %)))]
                      (ws/broadcast! [:apply-patch (fmt/get-edits (edit/diff db new-db))])
                      (dispatch [:change-turn])
                      new-db)
           db))))))

(on-event
 :end-trick
 (fn [db _]
   (let [tricks (get-in db [:rounds :current :tricks])
         trick  (:current tricks)
         winner (engine/winner trick)
         new-db (assoc-in db [:rounds :current :tricks]
                   (-> tricks
                       (update :past conj (assoc trick :winner winner))
                       (assoc :current (db/trick {:leader winner}))))]
     (ws/broadcast! [:apply-patch (fmt/get-edits (edit/diff db new-db))])
     (dispatch [:change-turn])
     new-db)))

(on-event
 :apply-patch
 (fn [db [_ diff :as event]]
   (when-let [ws-session (:ws-session (meta event))]
     (ws/broadcast! (remove #{ws-session} @ws/clients)
                    (pr-str event)))
   (edit/patch db (fmt/edits->script diff))))
