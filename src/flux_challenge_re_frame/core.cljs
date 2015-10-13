(ns ^:figwheel-always flux-challenge-re-frame.core
  (:require
    [flux-challenge-re-frame.handlers]
    [flux-challenge-re-frame.subscriptions]
    [flux-challenge-re-frame.db]
    [reagent.core :as reagent]
    [re-frame.core :as re-frame :refer [subscribe dispatch]]
    )
  )

(enable-console-print!)

;;; initial dispatches

(dispatch [:initialize-db])
(dispatch [:monitor-planets])
(dispatch [:populate-jedis])

;;; components

(defn planet-monitor [planet]
  [:h1 {:class "css-planet-monitor"}
   (str "Obi-Wan currently on " (:name planet))])

(defn jedi-slot [jedi]
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

(reagent/render-component
  [dark-jedi-list]
  (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
