(ns com.github.hindol.twenty-nine
  (:require
   [com.github.hindol.twenty-nine.events]
   [com.github.hindol.twenty-nine.subs]
   [com.github.hindol.twenty-nine.effects]
   [com.github.hindol.twenty-nine.views :as views]
   [reagent.dom :as rdom]
   [re-frame.core :as rf]))

(defn render
  []
  (rdom/render [views/ui] (.getElementById js/document "app")))

(defn ^:dev/after-load clear-cache-and-render!
  []
  (rf/clear-subscription-cache!)
  (render))

(defn ^:export init
  []
  (rf/dispatch-sync [:init])
  (render))