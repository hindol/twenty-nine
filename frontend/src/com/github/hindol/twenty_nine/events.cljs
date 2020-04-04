(ns com.github.hindol.twenty-nine.events
  (:require
   [com.github.hindol.twenty-nine.db :as db]
   [com.github.hindol.twenty-nine.engine :as engine]
   [com.github.hindol.twenty-nine.utils :refer [position]]
   [re-frame.core :as rf]
   [re-frame.std-interceptors :as i]
   [vimsical.re-frame.cofx.inject :as inject]))

(rf/reg-event-fx
 :init-db
 (fn [_ _]
   {:db       db/app-db
    :dispatch [:init-round]}))

(rf/reg-event-db
 :init-round
 (fn [db _]
   (assoc-in db [:rounds :current] (db/round))))

(rf/reg-event-fx
 :play
 [(i/path [:rounds :current])
  (rf/inject-cofx ::inject/sub [:turn])]
 (fn [{round :db
       turn  :turn} [_ player card]]
   (when  (= player turn)
     (let [candidates (engine/candidates (get-in round [:hands player])
                                         (get-in round [:tricks :current]))]
       (when (position #{card} candidates)
         {:db       (-> round
                        (update-in [:tricks :current :plays] assoc player card)
                        (update-in [:hands player] #(remove #{card} %)))
          :dispatch [:change-turn]})))))

(rf/reg-event-fx
 :change-turn
 [(i/path [:rounds :current :hands])
  (rf/inject-cofx ::inject/sub [:trick])
  (rf/inject-cofx ::inject/sub [:players])
  (rf/inject-cofx ::inject/sub [:turn])]
 (fn [{trick   :trick
       players :players
       hands   :db
       turn    :turn} _]
   (if (= :end-trick turn)
     {:dispatch-later [{:ms       1500
                        :dispatch [:end-trick]}]}
     (when (= :machine (get players turn))
       {:dispatch-later [{:ms       500
                          :dispatch [:play turn (engine/play (get hands turn) trick)]}]}))))

(rf/reg-event-fx
 :end-trick
 (i/path [:rounds :current :tricks])
 (fn [{tricks :db} _]
   (let [trick  (:current tricks)
         winner (engine/winner trick)]
     {:db       (-> tricks
                    (update :past conj (assoc trick :winner winner))
                    (assoc :current (db/trick {:leader winner})))
      :dispatch [:change-turn]})))
