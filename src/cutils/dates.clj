(ns

    ^{:doc    "cutils, date and time handling functions."
      :author "Paweł Wilk"}

    cutils.dates

  (:require [cutils.core]
            [clojure.string :as s]))

(cutils.core/init)

(def ^{:private true
       :const true
       :added "0.1"}
  mnames
  {:jan   1, :feb  2, :apr  4, :may  5, :jun  6, :jul  7,
   :aug   8, :sep  9, :oct 10, :nov 11, :dec 11,
   :mär   3, :mai  5, :okt 10, :dez 12,
   :led   1, :úno  2, :bře  3, :dub  4, :kvě  5, :čer  6,
   :srp   7, :zář  8, :říj  9, :pro 10, :bre  3, :cer  6,
   :fev   2, :zar  8, :rij  9, :kve  5, :aou  8,
   :fév   2, :avr  4, :juin 6, :juil 7, :aoû  8, :déc 12,
   :ene   1, :abr  4, :ago  8, :dic 12,
   :lan   1, :iun  6, :qui  7, :sex  8,
   :янв   1, :фев  2, :мар  3, :апр  4, :май  5, :июн  6,
   :июл   7, :авг  8, :сен  9, :окт 10, :ноя 11, :дек 12,
   :fie   2, :ijuń 6, :ijul 7, :awg  8, :cie  9, :noj 11,
   :die  12, :ejp  4, :mej  5, :dżun 6, :dżul 7, :ałg  8,
   :gen   1, :mag  5, :giu  6, :lug  7, :set  9, :ott 10,
   :ja    1, :fe   2, :ap   4, :au   8, :se   9, :oc  10,
   :no   11, :de  12,
   :st    1, :lu   2, :kw   4, :cz   6, :si   8, :wr   9,
   :pa  10,  :gr   12,
   :maa   3, :mei  5, :aŭg  8, :aŭ   8, :ok  10,
   :яну   1, :юн   6, :юл   7, :сеп  9, :ное 11,
   :ich   1, :ni   2, :san  3, :shig 4, :go   5, :ro   6,
   :shic  7, :ha   8, :ku   9, :jug 10, :jūg 10,
   :いちが 1, :にがつ 2,:さんが 3,:しがつ 4,:ごがつ 5,:ろくが 6,
   :しちが 7, :はちが 8,:くがつ 9,:じゅうが 10,:じゅうい 11,:じゅうに 12,
   :一月 1, :二月 2, :三月 3, :四月 4, :五月 5, :六月 6, :七月 7,
   :八月 8, :九月 9, :十月 10, :十一月 11, :十二月 12,
   :sty   1, :lut  2, :mar  3, :kwi  4, :maj  5, :cze  6,
   :lip   7, :sie  8, :wrz  9, :paź 10, :lis 11, :gru 12})

(defn- jmonthn
  [m]
  (some-> m s/join lower-case keyword mnames))

(defn month->num
  "Changes a name of a month to its numeric value."
  [m]
  (when-let [n (some->> m pstr (take 4))]
    (or (jmonthn n)
        (jmonthn (butlast n))
        (jmonthn (butlast (butlast n))))))
