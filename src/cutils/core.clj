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

(defmacro try-val
  {:added "1.0.0"}
  [val exclass & body]
  `(try (do ~@body) (catch ~exclass e# ~val)))

(defmacro try-arg
  [val & body]
  `(try-val ~val IllegalArgumentException ~@body))

(defmacro try-arg-false
  "Executes body catching IllegalArgumentException and if there is
  an exception it returns false."
  [& body]
  `(try-arg false ~@body))

(defmacro try-arg-nil
  "Executes body catching IllegalArgumentException and if there is
  an exception it returns nil."
  {:added "1.0.0"
   :tag clojure.lang.Fn}
  [& body]
  `(try-arg nil ~@body))
