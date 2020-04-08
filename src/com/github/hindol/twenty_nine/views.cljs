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
  (let [{plays :plays} @(rf/subscribe [:trick])
        columns        ["columns" "is-mobile" "is-centered" "is-variable" "is-3-desktop" "is-2-tablet" "is-1-mobile"]
        column         ["column" "is-1-desktop" "is-1-tablet" "is-3-mobile"]]
    [:div.columns
     [:div.column
      [:div {:class columns}
       [:div {:class column} [card (:north plays)]]]
      [:div {:class columns}
       [:div {:class column} [card (:west plays)]]
       [:div.is-offset-2 {:class column} [card (:east plays)]]]
      [:div {:class columns}
       [:div {:class column} [card (:south plays)]]]]]))

(defn show-hand
  [player]
  (let [hand  @(rf/subscribe [:hand player])
        cards (map (fn [c] [:div.column.is-1-desktop.is-1-tablet.is-3-mobile
                            [card c {:on-click #(rf/dispatch [:play player c])}]])
                   hand)]
    (into [:div.columns.is-mobile.is-centered.is-multiline.is-variable.is-3-desktop.is-2-tablet.is-1-mobile] cards)))

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
     [:button.button {:on-click #(rf/dispatch [:init-game])} "(Re)start"]
     [trick]
     [show-hand :south]
     [app-db]]]])
