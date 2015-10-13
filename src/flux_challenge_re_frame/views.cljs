(ns flux-challenge-re-frame.views
  (:require
    [re-frame.core :refer [subscribe]]
    )
  )

(defn- planet-monitor [planet]
  [:h1 {:class "css-planet-monitor"}
   (str "Obi-Wan currently on " (:name planet))])

(defn- jedi-slot [jedi]
  [:li {:class "css-slot"}
   (when jedi
     [:span
      [:h3 (:name jedi)]
      [:h6 (str "Homeworld: " (get-in jedi [:homeworld :name]))]])])

(defn dark-jedi-list []
  (let [current-planet (subscribe [:current-planet])
        padded-jedis (subscribe [:padded-jedis 5])]
    (fn []
      [:div {:class "css-root"}
       [planet-monitor @current-planet]

       [:section {:class "css-scrollable-list"}
        [:ul {:class "css-slots"}
         (for [jedi @padded-jedis]
           (let [key (or
                       (:name jedi)
                       (gensym "jedi"))] ; use random name if no name present
             ^{:key key} [jedi-slot jedi]))]

        [:div {:class "css-scroll-buttons"}
         [:button {:class "css-button-up"}]
         [:button {:class "css-button-down"}]]]])))
