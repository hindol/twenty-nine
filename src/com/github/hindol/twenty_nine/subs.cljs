(ns com.github.hindol.twenty-nine.subs
  (:require
   [clojure.pprint :as pp]
   [re-frame.core :as rf]))

; Debug only
(rf/reg-sub
 :app-db
 (fn [db _]
   db))

(rf/reg-sub
 :players
 (fn [db _]
   (:players db)))

(rf/reg-sub
 :rounds
 (fn [db _]
   (:rounds db)))

(rf/reg-sub
 :round
 :<- [:rounds]
 (fn [rounds _]
   (:current rounds)))

(rf/reg-sub
 :hands
 :<- [:round]
 (fn [round _]
   (:hands round)))

(rf/reg-sub
 :hand
 :<- [:hands]
 (fn [hands [_ player]]
   (get hands player)))

(rf/reg-sub
 :tricks
 :<- [:round]
 (fn [round _]
   (:tricks round)))

(rf/reg-sub
 :trick
 :<- [:tricks]
 (fn [tricks _]
   (:current tricks)))

(rf/reg-sub
 :turns
 :<- [:trick]
 (fn [trick _]
   (:turns trick)))

(rf/reg-sub
 :plays
 :<- [:trick]
 (fn [trick _]
   (:plays trick)))

(rf/reg-sub
 :turn
 :<- [:turns]
 :<- [:plays]
 (fn [[turns plays] _]
   (get turns (count plays) :end-trick)))