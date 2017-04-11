(ns documentation.cutils.overview)

[[:chapter {:title "Introduction"}]]

"
`cutils` is a library that adds some useful abstractions for managing collections in Clojure.

Currently provided macros, functions and dynamic variables are:

* **[`cutils.dates/`](#cutils.dates)**
  * [`month->num`](#month-num) – changes name of a month to its numeric value;   
   

* **[`cutils.digits/`](#cutils.digits)**
  * [`decimal-mark?`](#decimal-mark?) – checks if a character is decimal mark character,
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
  * [`num->digits`](#num-digits) – converts a number to a sequence of digits;   
   

  * [`*decimal-mark-mode*`](#var-decimal-mark-mode) – allows decimal mark character to appear during sequencing,
  * [`*decimal-mark-chars*`](#var-decimal-mark-chars) – defines a set of decimal mark characters,
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
   

  * [`with-decimal-mark-mode!`](#with-decimal-mark-mode!) – evaluates code with decimal mark mode enabled,
  * [`with-generic-mode!`](#with-generic-mode!) – evaluates code with numeric mode disabled,
  * [`with-numeric-mode!`](#with-numeric-mode!) – evaluates code with numeric mode enabled,
  * [`with-spread-numbers!`](#with-spread-numbers!) – evaluates code with spreading numbers enabled;
   

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

[[:file {:src "test/cutils/dates/month_num.clj" :tag "month-num-usage-ex"}]]

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

[[:section {:title "cutils.digits" :tag "cutils.digits"}]]

"
The `cutils.digits` namespace is a home for the `Digitizing` protocol. If
a data type implements this protocol it is able to express series of
digits.

Series of digits can be **generic** or **numeric** – it depends on what
they are composed of. Generic series of digits may contain multiple
separators and does not need to express a valid number. On the contrary
numeric series of digits must express a number and the only meaningful
elements that may appear in it (besides digits) are plus sign, minus signs
and a decimal mark (when decimal mark mode is in use).

Examples of generic series of digits:

* `\"555-11-123-55-10\"`
* `\"555-11 123-55-10\"`
* `[555 - 11 123 - 55 - 10]`
* `'(5 5 5 \".\" 1 1 \" \" 1 2 3 \"-\" 5 5 1 0)`
* `:1-2-3`
* `(symbol \"123\")`

Examples of numeric series of digits:

* `\"555 11 123 55 10\"`
* `\"-555 11 123 55 10\"`
* `\"+555 11 123 55 10\"`
* `[- 555 11 123 55 10]`
* `'(5 5 5 1 1 \" \" 1 2 3 5 5 1 0 \".\" 1)`
* `555111235510`
* `:123`

Long story short: numeric series of digits can be converted to a valid number.

Functions that are required to be defined when `Digitizing` protocol is implemented
for a data type should:

* Validate series by checking if they are generic series of digits ([`cutils.digits/digital?`](#digital?)).
* Validate series by checking if they are numeric series of digits ([`cutils.digits/numeric?`](#numeric?)).
* Normalize series by removing meaningless elements ([`cutils.digits/digitize`](#digitize)).
* Convert series to numbers ([`cutils.digits/digits->num`](#digits-num)).
* Convert series to strings ([`cutils.digits/digits->str`](#digits-str)).
* Convert series to (lazy) sequences ([`cutils.digits/digits->seq`](#digits-seq)).
* Fix trailing or preceding decimal marks in series ([`cutils.digits/digits-fix-dot`](#digits-fix-dot)).
* Check if series expresses negative value ([`cutils.digits/negative?`](#negative?)).
* Count digits in series ([`cutils.digits/count-digits`](#count-digits)).

The `cutils.digits` namespace contains functions implementing `Digitizing`
protocol for strings, vectors, sequences (objects implementing `ISeq`),
numbers (but no fractions), keywords, symbols and nil objects. It also
implements the protocol for `Object` type, providing fallback (by converting
values to strings and calling respective functions on the result).

The default mode for all functions is generic mode. To change the mode the
`cutils.digits/*numeric-mode*` dynamic variable should be set to
`true` (e.g. using `bindind` form or convenient macro
`cutils.digits/with-numeric-mode!`).

There are some exceptions: The functions `digits->num`, `numeric?` and
`negative?` will enforce numeric mode (the last one will enforce it but will
not validate whole sequence as numeric, just first characters) because it's
required for their correct operation. On the contrary the function `digital?`
will always enforce generic mode since it tests series for being digital, not
strictly numeric.

All protocol functions except `digits-fix-dot`, `digital?`, `numeric?` and
`fix-dot` will normalize input before operating on it and validate it which
may lead to raising an exception if it's malformed (e.g. series containing
strange elements, series expected to be numeric containing separators or
multiple sign symbols).
"

[[:subsection {:title "decimal-mark?" :tag "decimal-mark?"}]]

[[{:tag "decimal-mark-synopsis" :title "Synopsis" :numbered false}]]
(comment
  (cutils.digits/decimal-mark? c))

"
Returns `true` if the given argument is classified as a decimal mark
by searching a set `cutils.digits/*decimal-mark-chars*`.

If there is no match then it returns `false`.
"

[[:file {:src "test/cutils/digits/decimal_mark.clj" :tag "decimal-mark-ex"}]]


[[:subsection {:title "count-digits" :tag "count-digits"}]]

[[{:tag "count-digits-synopsis" :title "Synopsis" :numbered false}]]
(comment
  (cutils.digits/count-digits coll))

"
Counts the number of digits in series, normalizing and validating the input.
Returns a number.

When numeric mode is enabled (by
setting [`cutils.digits/*numeric-mode*`](#var-numeric-mode) to `true` or by
using [`cutils.digits/with-numeric-mode!`](#with-numeric-mode!)) the
validation is more strict. It means that the `+` or `-` sign must appear just
once, before any digit and the only valid separator (besides one of white
characters and `nil`) is a decimal mark (but only when decimal mark mode is
enabled by
setting [`cutils.digits/*decimal-mark-mode*`](#var-decimal-mark-mode) to
`true` or by
using [`cutils.digits/with-decimal-mark-mode!`](#with-decimal-mark-mode!)). If
the input series is malformed then an exception is raised.

By default generic mode is used during validation and normalization so the
input does not need to express correct number, just a bunch of digits and
separators.
"

[[:file {:src "test/cutils/digits/count_digits.clj" :tag "count-digits-ex"}]]


[[:subsection {:title "digital?" :tag "digital?"}]]

[[{:tag "digital-synopsis" :title "Synopsis" :numbered false}]]
(comment
  (cutils.digits/digital? coll))

"
Checks if the given object is digital. Returns `true` if it is, `false`
otherwise. Digital means that the series of elements consist of numbers
from 0 to 9 and optional separators.

This function normalizes the given object by
calling [`cutils.digits/digitize`](#digitize) and forces generic mode by
setting [`cutils.digits/*numeric-mode*`](#var-numeric-mode) to `false`.

If the input series is malformed no exception is raised but `false` value is
returned.
"

[[:file {:src "test/cutils/digits/digital.clj" :tag "digital-ex"}]]


[[:subsection {:title "digital-number?" :tag "digital-number?"}]]

[[{:tag "digital-number-synopsis" :title "Synopsis" :numbered false}]]
(comment
  (cutils.digits/digital-number? coll))

"
Returns `true` if the given number `n` can be used to express a collection of
digits with optional sign, `false` otherwise.

The function basically checks the type of the argument.
"

[[:file {:src "test/cutils/digits/digital_number.clj" :tag "digital-number-ex"}]]


[[:subsection {:title "digitize" :tag "digitize"}]]

[[{:tag "digitize-synopsis" :title "Synopsis" :numbered false}]]
(comment
  (cutils.digits/digitize coll))

"
Ensures that `coll` is digital by sanitizing it and performing basic
validation. If the process succeeds it returns a series of elements that are
normalized versions of values from `coll`. Normalized output consist of
numbers from 0 to 9 (as `Byte` objects) and optional `-` sign (as `Character`
object) in front (if numeric mode was in use and the sign was present in the
input series).

During normalization white characters are removed, digits (that might be
characters, numbers, strings, symbols or keys) are changed into their
numerical, byte representations and separators are preserved (if in generic
mode). If the dynamic
variable [`cutils.digits/*spread-numbers*`](#var-spread-numbers) is set to
`true` (default) then the function will spread the numbers that consist with
more than one digit by splitting the number and adding each digit separately
to the result.

When numeric mode is enabled (by
setting [`cutils.digits/*numeric-mode*`](#var-numeric-mode) to `true` or by
using [`cutils.digits/with-numeric-mode!`](#with-numeric-mode!)) the
validation is more strict. It means that the `+` or `-` sign must appear just
once, before any digit and the only valid separator (besides one of white
characters and `nil`) is a decimal mark (but only when decimal mark mode is
enabled by
setting [`cutils.digits/*decimal-mark-mode*`](#var-decimal-mark-mode) to
`true` or by
using [`cutils.digits/with-decimal-mark-mode!`](#with-decimal-mark-mode!)). If
the input series is malformed then an exception is raised.

By default generic mode is used during validation and normalization so the
input does not need to express correct number, just a bunch of digits and
separators.
"

[[:file {:src "test/cutils/digits/digitize.clj" :tag "digitize-ex"}]]


[[:subsection {:title "digits->num" :tag "digits-num"}]]

[[{:tag "digits-num-synopsis" :title "Synopsis" :numbered false}]]
(comment
  (cutils.digits/digits->num coll))

"

Changes a collection of digits given as coll into a number consisting of
decimal digits. An optional argument num-take controls how many digits to
use (from left to right) and num-drop tells how many digits to drop. The last
one (num-drop) is applied before num-take when both are given.

The input series is validated and normalized before performing further
operations on it by calling [`cutils.digits/digitize`](#digitize).

This function forces numeric mode by
setting [`cutils.digits/*numeric-mode*`](#var-numeric-mode) to `true` which
causes validation to be more strict.

If two or three arguments are given (the resulting number is going to be
sliced) then the first plus or minus character of the given collection will
not be taken into account during that operation (won't count when slicing).

The function returns a number or raises an exception if something went
wrong (e.g. input sequence was not valid). It returns `nil` if there was an
empty input or mismatched ranges.
"

[[:file {:src "test/cutils/digits/digits_num.clj" :tag "digits-num-ex"}]]
