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
 :ping
 (fn [_ _]
   {:ws-send {:message [:pong]}}))

(rf/reg-event-fx
 :init
 (fn [_ _]
   {:ws-connect {}}))

(rf/reg-event-fx
 :on-text
 (fn [_ [_ event]]
   {:dispatch event}))

(rf/reg-event-fx
 :send-diff
 (fn [{:keys [db]}]
   (let [diff (edit/diff @db/app-db db)]
     (reset! db/app-db db)
     {:ws-send {:message [:apply-patch (fmt/get-edits diff)]}})))

(rf/reg-event-db
 :apply-patch
 (fn [db [_ diff]]
   (reset! db/app-db (edit/patch db (fmt/edits->script diff)))))

(rf/reg-event-fx
 :init-db
 (fn [_ [_ db]]
   (reset! db/app-db db)
   {:db db}))

(rf/reg-event-fx
 :init-game
 (fn [_ _]
   {:ws-send {:message [:init-game]}}))

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
          :dispatch-n [[:send-diff]
                       [:change-turn]]})))))

(rf/reg-event-fx
 :change-turn
 (fn [_ _]
   {:ws-send {:message [:change-turn]}}))
