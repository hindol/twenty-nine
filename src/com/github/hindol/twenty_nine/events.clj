(ns com.github.hindol.twenty-nine.events
  (:require
   [clojure.core.async :as async]
   [clojure.stacktrace :as st]
   [com.github.hindol.twenty-nine.db :as db]
   [com.github.hindol.twenty-nine.engine :as engine]
   [com.github.hindol.twenty-nine.game :as game]
   [com.github.hindol.twenty-nine.ws :as ws]
   [editscript.core :as edit]
   [editscript.edit :as fmt])
  (:import
   (org.eclipse.jetty.websocket.api Session)))

(def event-handlers (atom {}))

(let [event-queue (async/chan 10)
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
                    (try
                      (swap! db/app-db handler dequeued)
                      (catch Exception e
                        (println "Handler for event: " (pr-str dequeued) " threw an exception!")
                        (st/print-stack-trace e))
                      (catch AssertionError e
                        (println "Handler for event: " (pr-str dequeued) " raised an assertion error!")
                        (st/print-stack-trace e)))
                    (println "No handler registered for event: " (pr-str dequeued-id)))
                  (recur)))))))

(defn dispatch-later
  [event {:keys [ms]}]
  (async/go
    (async/<! (async/timeout ms))
    (dispatch event)))

(defn on-event
  [id handler]
  (swap! event-handlers assoc id handler))

(on-event
 :pong
 (fn [db _]
   db))

(on-event
 :join-game
 (fn [db [_ {:keys [^Session ws-session]}]]
   (let [{:keys [uuid side]} (game/join {:ws-session ws-session})
         new-db              (assoc-in db [:players side] (str uuid))]
     (prn :join-game ws-session)
     (ws/broadcast! [:init-db (game/view-as new-db side)])
     new-db)))

(on-event
 :leave-game
 (fn [db [_ {:keys [^Session ws-session]}]]
   (let [side   (game/leave {:ws-session ws-session})
         new-db (assoc-in db [:players side] :machine)]
     (prn :leave-game ws-session)
     (ws/broadcast! [:apply-patch (fmt/get-edits (edit/diff db new-db))])
     new-db)))

(on-event
 :init-game
 (fn [db event]
   (let [{:keys [ws-session]} (meta event)
         new-db               (assoc db :rounds (db/game))]
     (ws/broadcast! [:init-db (game/view-as new-db (game/side ws-session))])
     new-db)))

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
                      (dispatch-later [:end-trick] {:ms 1500})
                      db)
         (case (get (:players db) turn)
           :machine (let [card (engine/play (get hands turn) trick)]
                      (dispatch-later [:play turn card] {:ms 500})
                      db)
           db))))))

(on-event
 :play
 (fn [db [_ turn card]]
   (let [new-db (-> db
                    (assoc-in [:rounds :current :tricks :current :plays turn] card)
                    (update-in [:rounds :current :hands turn] #(filterv (complement #{card}) %)))]
     (ws/broadcast! [:apply-patch (fmt/get-edits (edit/diff db new-db))])
     (dispatch [:change-turn])
     new-db)))

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
     (if (= 8 (count (get-in new-db [:rounds :current :tricks :past])))
       (dispatch [:end-round])
       (dispatch [:change-turn]))
     new-db)))

(on-event
 :end-round
 (fn [db _]
   (let [new-db (-> db
                    (update-in [:rounds :past] conj (get-in db [:rounds :current]))
                    (assoc-in [:rounds :current] (db/round)))]
     (ws/broadcast! [:apply-patch (fmt/get-edits (edit/diff db new-db))])
     new-db)))

(on-event
 :apply-patch
 (fn [db [_ diff :as event]]
   (when-let [ws-session (:ws-session (meta event))]
     (ws/broadcast! (remove #{ws-session} @ws/clients)
                    (pr-str event)))
   (edit/patch db (fmt/edits->script diff))))
