(ns documentation.cutils.overview)

[[:chapter {:title "Introduction"}]]

" `cutils` is a library that adds some abstractions for managing collections
in Clojure.

Currently provided macros and functions are:

* **[`cutils.dates/`](#cutils.dates)**
  * [`month->num`](#month-num) – changes name of a month to its numeric value;   

* **[`cutils.digits/`](#cutils.digits)**
  * [`digital?`](#digital?) – checks if a given collection expresses digits,
  * [`digital-number?`](#digital-number) – checks if a given, numeric object can express digit,
  * [`digitize`](#digitize) – ensures that a given collection is digital (expresses digits),
  * [`digits->num`](#digits-num) – converts a collection of digits to a number,
  * [`digits->seq`](#digits-seq) – converts a collection of digits to a lazy sequence,
  * [`digits->str`](#digits-str) – converts a collection of digits to a string,
  * [`num->digits`](#num-digits) – converts a number to a sequence of digits,
  * [`subs-signed`](#subs-signed) – creates a substring of digits preserving sign,
  * [`subseq-signed`](#subseq-signed) – creates a subsequence of digits preserving sign,
  * [`subvec-signed`](#subvec-signed) – creates a subvector of digits preserving sign;   

* **[`cutils.padding/`](#cutils.padding)**
  * [`pad`](#pad) – pads a collection with a given value,
  * [`pad-with-fn`](#pad-with-fn) – pads a collection with a result of a given function;  

* **[`cutils.ranges/`](#cutils.ranges)**
  * [`drop-take`](#drop-take) – calls `drop` and then `take` on a collection,
  * [`safe-range`](#safe-range) – safety wrapper for functions creating ranges,
  * [`safe-range-fn`](#safe-range-fn) – wraps a given function with a range-safety wrapper,
  * [`safe-subs`](#safe-subs) – safely creates a substring,
  * [`safe-subseq`](#safe-subseq) – safely creates a subsequence,
  * [`safe-subvec`](#safe-subvec) – safely creates a subvector,
  * [`subs-preserve`](#subs-preserve) – safely creates a substring preserving first character,
  * [`subseq-preserve`](#subseq-preserve) – safely creates a subsequence preserving first character,
  * [`subvec-preserve`](#subvec-preserve) – safely creates a subvector preserving first character,
  * [`vec-first`](#vec-first) – returns first element of a vector,
  * [`vec-first-idx`](#vec-first-idx) – returns index of first element of a vector for which predicate is true;  

* **[`cutils.strings/`](#cutils.strings)**
  * [`str-clean`](#str-clean) – removes white characters from beginning and end of a string,
  * [`remove-white-chars`](#remove-white-chars) – removes all white characters from a string,
  * [`str->int`](#str-int) – converts a given string to an integer,
  * [`str->num`](#str-num) – converts a given string to a number,
  * [`whitechar?`](#whitechar?) – checks if a given character is blank.
"

[[:chapter {:title "Installation"}]]

"Add dependencies to `project.clj`:

`[pl.randomseed/cutils `\"`{{PROJECT.version}}`\"`]`

Then (depending on which functions should be used) require it in your program:

`(require 'cutils.dates)`  
`(require 'cutils.digits)`  
`(require 'cutils.padding)`  
`(require 'cutils.ranges)`  
`(require 'cutils.strings)`  

or:

`(ns your-namespace`  
`  (:require [cutils.dates   :as dates])`  
`  (:require [cutils.digits  :as digits])`  
`  (:require [cutils.padding :as padding])`  
`  (:require [cutils.ranges  :as ranges])`  
`  (:require [cutils.strings :as strings]))`
"

[[:chapter {:title "Usage"}]]

[[:section {:title "cutils.dates" :tag "cutils.dates"}]]

"
The `cutils.dates` namespace contains functions that provide simple
conversions of dates."

[[:subsection {:title "month->num" :tag "month-num"}]]

[[{:tag "month-num-synopsis" :title "Synopsis" :numbered false}]]
(comment
  (cutils.dates/month->num m))

"
Changes a name of a month to its numeric value using first 4, 3 or 2 letters
of a given string.

If there is no match then it returns `nil`.
"

[[:file {:src "test/cutils/dates/month->num.clj" :tag "month-num-usage-ex"}]]

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

[[:section {:title "cutils.digits" :tag "cutils.digits"}]]

" The `cutils.digits` namespace is a home for the `Digitizing` protocol. If a
data type implements this protocol it is able to operate on collection(s) of
digits, especially:

* validate it by checking if it is a series of digits that can express a number,
* convert it to number,
* convert it to string,
* convert it to sequence of numbers.

Additionally `cutils.digits` contains functions that can be used to:

* check if a numeric value can be a valid digit in a collection,
* convert a number to a sequence of digits,
* subs-signed
* subseq-signed
* subvec-signed

  (digitize
   [coll]
   "Ensures that coll is digital by normalizing it and performing basic
validation. If the process succeeded it returns cleaned version of coll,
otherwise it returns nil. Digital means that the collection consist of
numbers from 0 to 9 (as numbers) and optional + or - sign (as a character)
in front.

Normalization means that white characters are removed, digits (that might be
                                                                    characters, numbers, strings, symbols or keys) are changed to their
numerical representations and first plus and minus signs (encoded as
                                                                  characters, strings, keywords, symbols or built-in function objects) are
changed into characters.")

  (digits->num
   [coll] [coll num-take] [coll num-drop num-take]
   "Changes a collection of digits given as coll into integer number. An
optional argument num-take controls how many digits to use (from left to
                                                                 right) and num-drop tells how many digits to drop before collecting
number. The last one (num-drop) is applied before num-take when both are
given.

Before slicing the collection is normalized (white characters are removed
                                                   and digits are changed into numerical representation) and validated (if the
collection contains other characters operation is stopped and nil is
returned). The first plus or minus character will not be taken into account
during slicing.

The function returns an integer or nil if something went wrong (e.g. empty
                                                                     collection was given or ranges were mismatched).")

  (digits->str
   [coll] [coll num-take] [coll num-drop num-take]
   "Changes a collection of digits given as coll into string containing
integer number. An optional argument num-take controls how many digits to
use (from left to right) and num-drop tells how many digits to drop before
collecting number. The last one (num-drop) is applied before num-take when
both are given.

Before slicing the collection is normalized (white characters are removed
                                                   and digits are changed into numerical representation) and validated (if the
collection contains other characters operation is stopped and nil is
returned). The first plus or minus character will not be taken into account
during slicing.

The function returns a string or nil if something went wrong (e.g. empty
                                                                   collection was given or ranges were mismatched).")

  (digits->seq
   [coll] [coll num-take] [coll num-drop num-take]
   "Changes a collection of digits given as coll into a sequence. An optional
argument num-take controls how many digits to use (from left to right) and
num-drop tells how many digits to drop before collecting number. The last
one (num-drop) is applied before num-take when both are given.

Before slicing the collection is normalized (white characters are removed
                                                   and digits are changed into numerical representation) and validated (if the
collection contains other characters operation is stopped and nil is
returned). The first plus or minus character will not be taken into account
during slicing.

The function returns a sequence or nil if something went wrong (e.g. empty
                                                                     collection was given or ranges were mismatched)."))
.

"

(defn digital-number?
  "Returns true if the given number n can be used to express a collection of
  digits with optional sign. Returns false otherwise."
  {:added "1.0.0"
   :const true
   :tag java.lang.Boolean}
  [^Number n]
  (contains? *digital-numbers* (class n)))

(defn subs-signed
  "Safely creates a substring preserving its first character when it is a plus
  or a minus sign. Preservation means that that sign (if present in front of
  the given string) is memorized and prepended to the resulting substring
  unless that substring is empty."
  {:added "1.0.0"
   :tag String}
  ([^String     s
    ^Number start]
   (subs-preserve s *sign-chars* start))
  ([^String     s
    ^Number start
    ^Number   num]
   (subs-preserve s *sign-chars* start (+ start num))))

(defn subseq-signed
  "Safely creates a sequence preserving its first character when it is a plus
  or a minus sign. Preservation means that that sign (if present in front of
  the given collection) is memorized and prepended to the resulting sequence
  unless that sequence is empty."
  {:added "1.0.0"
   :tag clojure.lang.ISeq}
  ([^clojure.lang.ISeq s
    ^Number        start]
   (subseq-preserve s *sign-chars* start))
  ([^clojure.lang.ISeq s
    ^Number        start
    ^Number          num]
   (subseq-preserve s *sign-chars* start (+ start num))))

(defn subvec-signed
  "Safely creates subvector preserving its first element when it is a plus or
  a minus sign. Preservation means that the sign (if present in front of the
  given vector) is always memorized and prepended to the resulting vector
  unless that vector is empty."
  {:added "1.0.0"
   :tag clojure.lang.IPersistentVector}
  ([^clojure.lang.IPersistentVector v
    ^Number start]
   (subvec-preserve v *sign-chars* start))
  ([^clojure.lang.IPersistentVector v
    ^Number start
    ^Number   num]
   (subvec-preserve v *sign-chars* start (+ start num))))

(defn num->digits
  "Changes a number given as n into a sequence of numbers representing decimal
  digits. If min-digits argument is given and it is larger than the number of
  digits then it pads the returned sequence with leading zeros to satisfy the
  number of elements with a first element preserved (and not counted) if it's
  not a digit.

  Be aware that in the second case the values of sequence
  elements will be evaluated in order to know their count."
  {:added "1.0.0"
   :tag clojure.lang.LazySeq}
  ([^Number n]
   (lazy-seq
    (if (neg? n)
      (cons \- (lazy-seq (num->digits-core n)))
      (if (zero? n)
        (list 0)
        (num->digits-core (*' -1 n))))))
  ([^Number n
    ^Number min-digits]
   (let [d (num->digits n)
         f (first d)]
     (if (digital-number? f)
       (pad d min-digits 0 true)
       (lazy-seq (cons f (pad (next d) (dec min-digits) 0 true)))))))
