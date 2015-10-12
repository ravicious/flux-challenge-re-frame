(ns ^:figwheel-always flux-challenge-re-frame.core
  (:require
    [clojure.walk :refer [keywordize-keys]]
    [reagent.core :as reagent :refer [atom]]
    [chord.client :refer [ws-ch]]
    [cljs.core.async :refer [<!]])
  (:require-macros
    [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

(defrecord Planet [id name])

(defonce app-state (atom {:planet (Planet. nil "unknown")
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

; Open a new websocket connection only if there's no planet in state.
; Without this we'd create a new websocket connection on each figwheel reload.
(when-not (get-in @app-state [:planet :id])
  (go
    (let [{:keys [ws-channel error]} (<! (ws-ch "ws://localhost:4000" {:format :json}))]
      (if-not error
        (loop []
          (let [response (<! ws-channel)
                error (:error response)
                planet (map->Planet (keywordize-keys (:message response)))]
            (if-not error
              (swap! app-state assoc :planet planet)
              (println (str "Error while receiving message: " error))))
          (recur))
        (println (str "Error while connecting: " error))))))

(defn planet-monitor [planet]
  [:h1 {:class "css-planet-monitor"}
   (str "Obi-Wan currently on " (:name planet))])

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
