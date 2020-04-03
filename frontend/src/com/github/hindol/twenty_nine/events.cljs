(ns com.github.hindol.twenty-nine.events
  (:require
   [clojure.pprint :as pp]
   [com.github.hindol.twenty-nine.db :as db]
   [re-frame.core :as rf]
   [re-frame.std-interceptors :as i]
   [vimsical.re-frame.cofx.inject :as inject]))

(rf/reg-event-db
 :init-db
 (fn [_ _]
   db/app-db))

(rf/reg-event-fx
 :play
 [(i/path [:rounds :current]) (rf/inject-cofx ::inject/sub [:turn])]
 (fn [{round :db
       turn  :turn} [_ player card]]
   {:db       (cond-> round
                (= player turn) (->
                                 (update-in [:tricks :current :plays] assoc player card)
                                 (update-in [:hands player] #(remove #{card} %))))
    :dispatch [:change-turn]}))

(rf/reg-event-fx
 :change-turn
 [(i/path [:rounds :current :hands])
  (rf/inject-cofx ::inject/sub [:players])
  (rf/inject-cofx ::inject/sub [:turn])]
 (fn [{players :players
       hands   :db
       turn    :turn} _]
   (if (= :end-trick turn)
     {:dispatch-later [{:ms       1500
                        :dispatch [:end-trick]}]}
     (when (= :machine (get players turn))
       {:dispatch-later [{:ms       500
                          :dispatch [:play turn (rand-nth (get hands turn))]}]}))))

(rf/reg-event-fx
 :end-trick
 (i/path [:rounds :current :tricks])
 (fn [{tricks :db} _]
   {:db (-> tricks
            (update :past conj (:current tricks))
            (assoc :current (db/trick {:leader :south})))}))