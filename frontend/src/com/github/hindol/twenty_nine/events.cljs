(ns com.github.hindol.twenty-nine.events
  (:require
   [clojure.pprint :as pp]
   [com.github.hindol.twenty-nine.db :as db]
   [re-frame.core :as rf]
   [re-frame.std-interceptors :as i]))

(rf/reg-event-db
 :init-db
 (fn [_ _]
   db/app-db))

(rf/reg-event-db
 :play
 (i/path [:rounds :current])
 (fn [round [_ card {:keys [player turn]}]]
   (cond-> round
     (= player turn) (->
                      (update-in [:tricks :current :plays] assoc player card)
                      (update-in [:hands player] #(remove #{card} %))))))