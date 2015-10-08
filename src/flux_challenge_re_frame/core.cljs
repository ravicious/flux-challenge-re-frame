(ns ^:figwheel-always flux-challenge-re-frame.core
  (:require
    [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:planet "Tatooine"
                          :jedis [{:name "Jorak Uln"
                                   :homeworld "Korriban"}
                                  {:name "Exar Kun"
                                   :homeworld "Coruscant"}
                                  {:name "Skere Kaan"
                                   :homeworld "Coruscant"}
                                  {:name "Na'daz"
                                   :homeworld "Ryloth"}
                                  {:name "Darth Bane"
                                   :homeworld "Apatros"}]}))

(defn planet-monitor [planet]
  [:h1 {:class "css-planet-monitor"}
   (str "Obi-Wan currently on " planet)])

(defn jedi-slot [jedi]
  [:li {:class "css-slot"}
   [:h3 (:name jedi)]
   [:h6 (str "Homeworld: " (:homeworld jedi))]])

(defn dark-jedi-list []
  [:div {:class "css-root"}
   [planet-monitor (:planet @app-state)]

   [:section {:class "css-scrollable-list"}
    [:ul {:class "css-slots"}
     (for [jedi (:jedis @app-state)]
       ^{:key (:name jedi)} [jedi-slot jedi])]

    [:div {:class "css-scroll-buttons"}
     [:button {:class "css-button-up"}]
     [:button {:class "css-button-down"}]]]])

(reagent/render-component
  [dark-jedi-list]
  (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
