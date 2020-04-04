(ns com.github.hindol.twenty-nine.views
  (:require
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [re-frame.core :as rf]))

(defn url-for
  [{:keys [suit rank]}]
  (str "images/cards/minified/"
       (name suit) "/"
       (let [r (str/lower-case (name rank))]
         (case r
           "ace"   "1"
           "jack"  "j"
           "queen" "q"
           "king"  "k"
           r))
       (str/lower-case (subs (name suit) 0 1)) ".svg"))

(defn card
  [c props]
  (when c
    [:figure.image.is-fullwidth
     [:img (merge {:src (url-for c)} props)]]))

(defn trick
  []
  (let [{plays :plays} @(rf/subscribe [:trick])]
    [:div.columns
     [:div.column
      [:div.columns.is-mobile.is-centered
       [:div.column.is-1 [card (:north plays)]]]
      [:div.columns.is-mobile.is-centered
       [:div.column.is-1 [card (:west plays)]]
       [:div.column.is-1.is-offset-2 [card (:east plays)]]]
      [:div.columns.is-mobile.is-centered
       [:div.column.is-1 [card (:south plays)]]]]]))

(defn show-hand
  [player]
  (let [hand  @(rf/subscribe [:hand player])
        cards (map (fn [c] [:div.column.is-1
                            [card c {:on-click #(rf/dispatch [:play player c])}]])
                   hand)]
    (into [:div.columns.is-mobile.is-centered] cards)))

(defn app-db
  []
  [:div.columns
   [:div.column
    [:pre (with-out-str (pp/pprint @(rf/subscribe [:app-db])))]]])

(defn ui
  []
  [:div.container.is-fluid
   [:div.columns
    [:div.column
     [trick]
     [show-hand :south]
     [app-db]]]])
