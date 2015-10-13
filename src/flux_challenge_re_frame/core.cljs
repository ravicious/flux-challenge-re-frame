(ns ^:figwheel-always flux-challenge-re-frame.core
  (:require
    [flux-challenge-re-frame.handlers]
    [flux-challenge-re-frame.subscriptions]
    [flux-challenge-re-frame.db]
    [flux-challenge-re-frame.views :as views]
    [reagent.core :as reagent]
    [re-frame.core :as re-frame]
    )
  )

(enable-console-print!)

;;; initial dispatches

(re-frame/dispatch [:initialize-db])
(re-frame/dispatch [:monitor-planets])
(re-frame/dispatch [:populate-jedis])

(reagent/render-component
  [views/dark-jedi-list]
  (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
