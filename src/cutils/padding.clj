(ns

    ^{:doc    "cutils, safe padding."
      :author "Paweł Wilk"}

    cutils.ranges)

(cutils.core/init)

(defn pad-with-fn
  "Pads a collection given as coll with results of calling f. The number of
  elements added to a resulting collection will be calculated using the value
  of upto argument which specifies how large the collection should ultimately
  be.

  The given function f should take one argument which will contain integer
  index of currently generated element, starting from the index of first
  padded element. Example:

  (pad-with-fn [0 1 2] 5) ;=> [0 1 2 3 4]

  If no f is given then the padding is done with identity function which
  effectively puts index numbers of elements as elements.

  If the optional, fourth argument front? is true then padding values will be
  added to the beginning of a resulting collection. That also changes values
  of an argument passed during successive f calls as they will start with
  negative number and grow to -1. Example:

  (pad-with-fn identity [0 1 2] 5 true) ;=> [-2 -1 0 1 2]

  If the value of coll is a vector then a proper functions will be used and
  the result will also be a vector. Be aware that prepending to vectors (when
  front? is set to true) will internally create temporary vector."
  ([^clojure.lang.ISeq coll
    ^long              upto]
   (pad-with-fn identity coll upto false))
  ([^clojure.lang.Fn   f
    ^clojure.lang.ISeq coll
    ^long              upto]
   (pad-with-fn f coll upto false))
  ([^clojure.lang.Fn   f
    ^clojure.lang.ISeq coll
    ^long              upto
    ^java.lang.Boolean front?]
   (let [ve? (vector? coll)
         iop (if ve? into concat)
         rng (if front?
               (range (- (count coll) upto) 0)
               (range (count coll) upto))
         pdv (not-empty (map f rng))
         src (if front?
               (list (if ve? (vec pdv) pdv) coll)
               (list coll pdv))]
     (apply iop src))))

(defn pad
  "Pads a collection given as coll with a value given as val. The number of
  elements added to a resulting collection will be calculated using the value
  of upto argument which specifies how large the collection should ultimately
  be. Examples:

  (pad [0 1 2] 5)     ;=> [0 1 2 nil nil]
  (pad [0 1 2] 5 :pa) ;=> [0 1 2 :pa :pa]

  If no val is given then the padding is done using nil values.

  If the optional, fourth argument front? is true then padding values will be
  added to the beginning of a resulting collection. Example:

  (pad [0 1 2] 5 :pa true) ;=> [:pa :pa 0 1 2]

  If the value of coll is a vector then a proper functions will be used and
  the result will also be a vector. Be aware that prepending to vectors (when
  front? is set to true) will internally create temporary vector."
  ([^clojure.lang.ISeq coll ^long upto]
   (pad-with-fn (constantly nil) coll upto))
  ([^clojure.lang.ISeq coll ^long upto val]
   (pad-with-fn (constantly val) coll upto))
  ([^clojure.lang.ISeq coll ^long upto val ^java.lang.Boolean front?]
   (pad-with-fn (constantly val) coll upto front?)))
