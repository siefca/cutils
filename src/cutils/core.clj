(ns

    ^{:doc    "cutils library, core imports."
      :author "Paweł Wilk"}

    cutils.core

  (:require [environ.core :refer [env]]))

(defn init
  []
  (when (env :dev-mode)
    (set! *warn-on-reflection* true)))
