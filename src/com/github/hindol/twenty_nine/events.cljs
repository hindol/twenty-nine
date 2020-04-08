(ns com.github.hindol.twenty-nine.events
  (:require
   [com.github.hindol.twenty-nine.db :as db]
   [com.github.hindol.twenty-nine.engine :as engine]
   [com.github.hindol.twenty-nine.utils :refer [position]]
   [editscript.core :as edit]
   [editscript.edit :as fmt]
   [re-frame.core :as rf]
   [re-frame.std-interceptors :as i]
   [vimsical.re-frame.cofx.inject :as inject]))

(rf/reg-event-fx
 :init
 (fn [_ _]
   {:connect-ws {}}))

(rf/reg-event-fx
 :on-text
 (fn [_ [_ event]]
   {:dispatch event}))

(rf/reg-event-fx
 :send-diff
 (fn [{:keys [db]}]
   (let [diff (edit/diff @db/app-db db)]
     (reset! db/app-db db)
     {:send-ws {:message [:apply-patch (fmt/get-edits diff)]}})))

(rf/reg-event-db
 :apply-patch
 (fn [db [_ diff]]
   (reset! db/app-db (edit/patch db (fmt/edits->script diff)))))

(rf/reg-event-fx
 :init-db
 (fn [_ [_ db]]
   (reset! db/app-db db)
   {:db       db
    :dispatch [:init-round]}))

(rf/reg-event-fx
 :init-round
 (i/path [:rounds :current])
 (fn [_ _]
   {:db       (db/round)
    :dispatch [:send-diff]}))

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
         {:db         (-> round
                          (update-in [:tricks :current :plays] assoc player card)
                          (update-in [:hands player] #(remove #{card} %)))
          :dispatch-n [[:change-turn]
                       [:send-diff]]})))))

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
     {:db         (-> tricks
                      (update :past conj (assoc trick :winner winner))
                      (assoc :current (db/trick {:leader winner})))
      :dispatch-n [[:change-turn]
                   [:send-diff]]})))