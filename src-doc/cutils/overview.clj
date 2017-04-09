(ns documentation.cutils.overview)

[[:chapter {:title "Introduction"}]]

"
`cutils` is a library that adds some useful abstractions for managing collections in Clojure.

Currently provided macros, functions and dynamic variables are:

* **[`cutils.dates/`](#cutils.dates)**
  * [`month->num`](#month-num) – changes name of a month to its numeric value;   

* **[`cutils.digits/`](#cutils.digits)**
  * [`decimal-point-char?`](#decimal-point-char?) – checks if a character is decimal point character,
  * [`count-digits`](#count-digits) – counts the number of digits in a digital collection,
  * [`digit?`](#digit?) – checks if a given object is a digit,
  * [`digital?`](#digital?) – checks if a given collection expresses series of digits with optional separators,
  * [`digital-number?`](#digital-number) – checks if a given, numeric object can express digit,
  * [`digitize`](#digitize) – ensures that a given collection is digital (expresses digits),
  * [`digits->num`](#digits-num) – converts a collection of digits to a number,
  * [`digits->seq`](#digits-seq) – converts a collection of digits to a lazy sequence,
  * [`digits->str`](#digits-str) – converts a collection of digits to a string,
  * [`digits-fix-dot`](#digits-fix-dot) – fixes orphaned dots in digital collection,
  * [`dot?`](#dot) – returns `true` if character is defined dot character,
  * [`negative?`](#negative?) – checks if a collection expresses negative number,
  * [`numeric?`](#numeric?) – checks if a collection expresses (can be converted to) a number,
  * [`num->digits`](#num-digits) – converts a number to a sequence of digits,   

  * [`*decimal-point-mode*`](#var-decimal-point-mode) – allows decimal point character to appear during sequencing,
  * [`*decimal-point-chars*`](#var-decimal-point-chars) – defines a set of decimal point characters,
  * [`*digital-numbers*`](#var-digital-numbers) – defines a set of types that are valid representations of digits,
  * [`*dot-char*`](#var-dot-char) – defines a dot character,
  * [`*minus-signs*`](#var-minus-signs) – defines a set of values that are recognized as minus signs,
  * [`*numeric-mode*`](#var-numeric-mode) – enables numeric mode during sequencing,
  * [`*separator-chars*`](#var-separator-chars) – defines an alternative set of separator characters,
  * [`*separator-classes*`](#var-separator-classes) – defines a set of separator classes,
  * [`*sign-to-char*`](#var-sign-to-char) – defines a map for numeric signs disambiguation,
  * [`*signs*`](#var-signs) – defines a set of known numeric signs (plus and minus),
  * [`*spread-numbers*`](#var-spread-numbers) – enables spreading numbers into digits during sequencing,
  * [`*white-chars*`](#var-white-chars) – defines a set of common blank characters,
  * [`*vals-to-digits*`](#var-vals-to-digits) – defines a map for digits disambiguation;   

  * [`with-decimal-point-mode!`](#with-decimal-point-mode) – evaluates code with decimal-point mode enabled,
  * [`with-generic-mode!`](#with-generic-mode) – evaluates code with numeric mode disabled,
  * [`with-numeric-mode!`](#with-numeric-mode) – evaluates code with numeric mode enabled,
  * [`with-spread-numbers!`](#with-spread-numbers) – evaluates code with spreading numbers enabled;

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
Changes a name of a month to its numeric value (position number in
Gregorian calendar) using first 4, 3 or 2 letters of a given string.

If there is no match then it returns `nil`.
"

[[:file {:src "test/cutils/dates/month-num.clj" :tag "month-num-usage-ex"}]]

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

[[:section {:title "cutils.digits" :tag "cutils.digits"}]]

"
The `cutils.digits` namespace is a home for the `Digitizing` protocol. If
a data type implements this protocol it is able to operate on collections of
digits, especially:

* Validate it by checking if it is a series of digits and optional separators.
* Validate it by checking if it is a series of digits that can express a number.
* Normalize it by removing white characters.
* Convert it to a number.
* Convert it to a string.
* Convert it to a lazy sequence.

Additionally `cutils.digits` contains functions that can be used to:

* Check if a numeric value can be a valid digit in a collection.
* Convert a number to a sequence of digits.
* Count number of digits in a digital collection.
"

[[:subsection {:title "decimal-point-char?" :tag "decimal-point-char"}]]

[[{:tag "decimal-point-char-synopsis" :title "Synopsis" :numbered false}]]
(comment
  (cutils.digits/decimal-point-char? c))

"
Returns `true` if the given argument is a character classified as
a decimal point separator by searching a set `cutils.digits/*decimal-point-chars*`.

If there is no match then it returns `false`.
"

[[:file {:src "test/cutils/digits/decimal-point-char.clj" :tag "decimal-point-char-ex"}]]
