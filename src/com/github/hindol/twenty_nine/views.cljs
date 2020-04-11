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
  [:figure.image.is-fullwidth
   (if c
     [:img (merge {:src (url-for c)} props)]
     [:img (merge {:src   (url-for {:suit :clubs
                                    :rank :2})
                   :style {:visibility :hidden}} props)])])

(defn trick
  []
  (let [{plays :plays} @(rf/subscribe [:trick])
        columns        ["columns" "is-mobile" "is-centered" "is-variable" "is-1"]
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

(defn cards
  [cs {:keys [on-click]}]
  (let [columns ["columns" "is-mobile" "is-centered" "is-multiline" "is-variable" "is-1"]
        column  ["column" "is-1-desktop" "is-1-tablet" "is-3-mobile"]]
    [:div {:class columns}
     (if (empty? cs)
       [:div {:class column}
        [card {:suit :clubs
               :rank :2} {:style {:visibility :hidden}}]]
       (map-indexed (fn [idx c]
                      ^{:key idx}
                      [:div {:class column}
                       [card c {:on-click #(on-click c)}]])
                    cs))]))

(defn show-hand
  [player]
  (let [hand @(rf/subscribe [:hand player])]
    [:div.columns
     [:div.column
      [cards hand {:on-click #(rf/dispatch [:play player %])}]]]))

(defn controls
  []
  [:div.level
   [:div.level-left
    [:div.level-item
     [:button.button {:on-click #(rf/dispatch [:init-game])} "New Game"]]]])

(defn app-db
  []
  [:div.columns
   [:div.column
    [:pre (with-out-str (pp/pprint @(rf/subscribe [:app-db])))]]])

(defn ui
  []
  [:section.section
   [:div.container.is-fluid
    [:div.columns
     [:div.column
      [controls]
      [trick]
      [show-hand :south]
      [app-db]]]]])
