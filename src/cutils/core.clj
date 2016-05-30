(ns

    ^{:doc    "cutils library, core imports."
      :author "Paweł Wilk"}

    cutils.core

  (:require [environ.core :refer [env]]))

(defn init
  {:added "1.0.0"
   :tag nil}
  []
  (when (env :dev-mode)
    (set! *warn-on-reflection* true)))

(defn throw-arg
  {:added "1.0.0"
   :tag nil}
  [& msgs]
  (throw (IllegalArgumentException. ^String (apply str msgs))))
