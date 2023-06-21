version 1.0

workflow end_at_timestamp {
  input {

    # Timestamp in "seconds since epoch". You can find the current value this will be compared
    # against by running 'date +%s'
    Int final_timestamp

    String input1
    String input2
    String input3
    String input4
    String input5
    String input6
    String input7
    String input8
    String input9
    String input10

    String input11
    String input12
    String input13
    String input14
    String input15
    String input16
    String input17
    String input18
    String input19
    String input20

    String input21
    String input22
    String input23
    String input24
    String input25
    String input26
    String input27
    String input28
    String input29
    String input30

    String input31
    String input32
    String input33
    String input34
    String input35
    String input36
    String input37
    String input38
    String input39
    String input40

    String input41
    String input42
    String input43
    String input44
    String input45
    String input46
    String input47
    String input48
    String input49
    String input50

    String input51
    String input52
    String input53
    String input54
    String input55
    String input56
    String input57
    String input58
    String input59
    String input60

    String input61
    String input62
    String input63
    String input64
    String input65
    String input66
    String input67
    String input68
    String input69
    String input70

    String input71
    String input72
    String input73
    String input74
    String input75
    String input76
    String input77
    String input78
    String input79
    String input80

    String input81
    String input82
    String input83
    String input84
    String input85
    String input86
    String input87
    String input88
    String input89
    String input90

    String input91
    String input92
    String input93
    String input94
    String input95
    String input96
    String input97
    String input98
    String input99
    String input100

    Int input101
    Int input102
    Int input103
    Int input104
    Int input105
    Int input106
    Int input107
    Int input108
    Int input109
    Int input110

    Int input111
    Int input112
    Int input113
    Int input114
    Int input115
    Int input116
    Int input117
    Int input118
    Int input119
    Int input120

    Int input121
    Int input122
    Int input123
    Int input124
    Int input125
    Int input126
    Int input127
    Int input128
    Int input129
    Int input130

    Int input131
    Int input132
    Int input133
    Int input134
    Int input135
    Int input136
    Int input137
    Int input138
    Int input139
    Int input140

    Int input141
    Int input142
    Int input143
    Int input144
    Int input145
    Int input146
    Int input147
    Int input148
    Int input149
    Int input150

    Int input151
    Int input152
    Int input153
    Int input154
    Int input155
    Int input156
    Int input157
    Int input158
    Int input159
    Int input160

    Int input161
    Int input162
    Int input163
    Int input164
    Int input165
    Int input166
    Int input167
    Int input168
    Int input169
    Int input170

    Int input171
    Int input172
    Int input173
    Int input174
    Int input175
    Int input176
    Int input177
    Int input178
    Int input179
    Int input180

    Int input181
    Int input182
    Int input183
    Int input184
    Int input185
    Int input186
    Int input187
    Int input188
    Int input189
    Int input190

    Boolean input191
    Boolean input192
    Boolean input193
    Boolean input194
    Boolean input195
    Boolean input196
    Boolean input197
    Boolean input198
    Boolean input199
  }

  call sleep_until_timestamp { input: final_timestamp = final_timestamp }

  output {
    String output1 = "foo"
    String output2 = "foo"
    String output3 = "foo"
    String output4 = "foo"
    String output5 = "foo"
    String output6 = "foo"
    String output7 = "foo"
    String output8 = "foo"
    String output9 = "foo"
    String output10 = "foo"

    String output11 = "foo"
    String output12 = "foo"
    String output13 = "foo"
    String output14 = "foo"
    String output15 = "foo"
    String output16 = "foo"
    String output17 = "foo"
    String output18 = "foo"
    String output19 = "foo"
    String output20 = "foo"

    String output21 = "foo"
    String output22 = "foo"
    String output23 = "foo"
    String output24 = "foo"
    String output25 = "foo"
    String output26 = "foo"
    String output27 = "foo"
    String output28 = "foo"
    String output29 = "foo"
    String output30 = "foo"

    String output31 = "foo"
    String output32 = "foo"
    String output33 = "foo"
    String output34 = "foo"
    String output35 = "foo"
    String output36 = "foo"
    String output37 = "foo"
    String output38 = "foo"
    String output39 = "foo"
    String output40 = "foo"

    String output41 = "foo"
    String output42 = "foo"
    String output43 = "foo"
    String output44 = "foo"
    String output45 = "foo"
    String output46 = "foo"
    String output47 = "foo"
    String output48 = "foo"
    String output49 = "foo"
    String output50 = "foo"

    String output51 = "foo"
    String output52 = "foo"
    String output53 = "foo"
    String output54 = "foo"
    String output55 = "foo"
    String output56 = "foo"
    String output57 = "foo"
    String output58 = "foo"
    String output59 = "foo"
    String output60 = "foo"

    String output61 = "foo"
    String output62 = "foo"
    String output63 = "foo"
    String output64 = "foo"
    String output65 = "foo"
    String output66 = "foo"
    String output67 = "foo"
    String output68 = "foo"
    String output69 = "foo"
    String output70 = "foo"

    String output71 = "foo"
    String output72 = "foo"
    String output73 = "foo"
    String output74 = "foo"
    String output75 = "foo"
    String output76 = "foo"
    String output77 = "foo"
    String output78 = "foo"
    String output79 = "foo"
    String output80 = "foo"

    String output81 = "foo"
    String output82 = "foo"
    String output83 = "foo"
    String output84 = "foo"
    String output85 = "foo"
    String output86 = "foo"
    String output87 = "foo"
    String output88 = "foo"
    String output89 = "foo"
    String output90 = "foo"

    String output91 = "foo"
    String output92 = "foo"
    String output93 = "foo"
    String output94 = "foo"
    String output95 = "foo"
    String output96 = "foo"
    String output97 = "foo"
    String output98 = "foo"
    String output99 = "foo"
    String output100 = "foo"

    Int output101 = 1010
    Int output102 = 1010
    Int output103 = 1010
    Int output104 = 1010
    Int output105 = 1010
    Int output106 = 1010
    Int output107 = 1010
    Int output108 = 1010
    Int output109 = 1010
    Int output110 = 1010

    Int output111 = 1010
    Int output112 = 1010
    Int output113 = 1010
    Int output114 = 1010
    Int output115 = 1010
    Int output116 = 1010
    Int output117 = 1010
    Int output118 = 1010
    Int output119 = 1010
    Int output120 = 1010

    Int output121 = 1010
    Int output122 = 1010
    Int output123 = 1010
    Int output124 = 1010
    Int output125 = 1010
    Int output126 = 1010
    Int output127 = 1010
    Int output128 = 1010
    Int output129 = 1010
    Int outpu130 = 1010

    Int output131 = 1010
    Int output132 = 1010
    Int output133 = 1010
    Int output134 = 1010
    Int output135 = 1010
    Int output136 = 1010
    Int output137 = 1010
    Int output138 = 1010
    Int output139 = 1010
    Int output140 = 1010

    Int output141 = 1010
    Int output142 = 1010
    Int output143 = 1010
    Int output144 = 1010
    Int output145 = 1010
    Int output146 = 1010
    Int output147 = 1010
    Int output148 = 1010
    Int output149 = 1010
    Int output150 = 1010

    Int output151 = 1010
    Int output152 = 1010
    Int output153 = 1010
    Int output154 = 1010
    Int output155 = 1010
    Int output156 = 1010
    Int output157 = 1010
    Int output158 = 1010
    Int output159 = 1010
    Int output160 = 1010

    Int output161 = 1010
    Int output162 = 1010
    Int output163 = 1010
    Int output164 = 1010
    Int output165 = 1010
    Int output166 = 1010
    Int output167 = 1010
    Int output168 = 1010
    Int output169 = 1010
    Int output170 = 1010

    Int output171 = 1010
    Int output172 = 1010
    Int output173 = 1010
    Int output174 = 1010
    Int output175 = 1010
    Int output176 = 1010
    Int output177 = 1010
    Int output178 = 1010
    Int output179 = 1010
    Int output180 = 1010

    Int output181 = 1010
    Int output182 = 1010
    Int output183 = 1010
    Int output184 = 1010
    Int output185 = 1010
    Int output186 = 1010
    Int output187 = 1010
    Int output188 = 1010
    Int output189 = 1010
    Int output190 = 1010

    Int output191 = 1010
    Int output192 = 1010
    Int output193 = 1010
    Int output194 = 1010
    Int output195 = 1010
    Int output196 = 1010
    Int output197 = 1010
    Int output198 = 1010
    Int output199 = 1010
    Int output200 = 1010

    Boolean output201 = false
    Boolean output202 = false
    Boolean output203 = false
    Boolean output204 = false
    Boolean output205 = false
    Boolean output206 = false
    Boolean output207 = false
    Boolean output208 = false
    Boolean output209 = false
    Boolean output210 = false

    Boolean output211 = false
    Boolean output212 = false
    Boolean output213 = false
    Boolean output214 = false
    Boolean output215 = false
    Boolean output216 = false
    Boolean output217 = false
    Boolean output218 = false
    Boolean output219 = false
    Boolean output220 = false

    Boolean output221 = false
    Boolean output222 = false
    Boolean output223 = false
    Boolean output224 = false
    Boolean output225 = false
    Boolean output226 = false
    Boolean output227 = false
    Boolean output228 = false
    Boolean output229 = false
    Boolean output230 = false

    Boolean output231 = false
    Boolean output232 = false
    Boolean output233 = false
    Boolean output234 = false
    Boolean output235 = false
    Boolean output236 = false
    Boolean output237 = false
    Boolean output238 = false
    Boolean output239 = false
    Boolean output240 = false

    Boolean output241 = false
    Boolean output242 = false
    Boolean output243 = false
    Boolean output244 = false
    Boolean output245 = false
    Boolean output246 = false
    Boolean output247 = false
    Boolean output248 = false
    Boolean output249 = false
    Boolean output250 = false

    Boolean output251 = false
    Boolean output252 = false
    Boolean output253 = false
    Boolean output254 = false
    Boolean output255 = false
    Boolean output256 = false
    Boolean output257 = false
    Boolean output258 = false
    Boolean output259 = false
    Boolean output260 = false

    Boolean output261 = false
    Boolean output262 = false
    Boolean output263 = false
    Boolean output264 = false
    Boolean output265 = false
    Boolean output266 = false
    Boolean output267 = false
    Boolean output268 = false
    Boolean output269 = false
    Boolean output270 = false

    Boolean output271 = false
    Boolean output272 = false
    Boolean output273 = false
    Boolean output274 = false
    Boolean output275 = false
    Boolean output276 = false
    Boolean output277 = false
    Boolean output278 = false
    Boolean output279 = false
    Boolean output280 = false

    Boolean output281 = false
    Boolean output282 = false
    Boolean output283 = false
    Boolean output284 = false
    Boolean output285 = false
    Boolean output286 = false
    Boolean output287 = false
    Boolean output288 = false
    Boolean output289 = false
    Boolean output290 = false

    Boolean output291 = false
    Boolean output292 = false
    Boolean output293 = false
    Boolean output294 = false
    Boolean output295 = false
    Boolean output296 = false
    Boolean output297 = false
    Boolean output298 = false
    Boolean output299 = false
    Boolean output300 = false
  }
}

task sleep_until_timestamp {
  input {
    Int final_timestamp
  }
  command <<<
    SLEEPY_TIME=$(( ~{final_timestamp} - $(date +%s) ))
    if [[ "${SLEEPY_TIME}" -gt 0 ]] && [[ "${SLEEPY_TIME}" -lt 3600  ]]
    then
    echo "Sleeping for ${SLEEPY_TIME}"
    sleep ${SLEEPY_TIME}
    else
    echo "Invalid sleepy time: ${SLEEPY_TIME}"
    fi

  >>>
  runtime {
    docker: "ubuntu:latest"
  }
  output {
  }
}
