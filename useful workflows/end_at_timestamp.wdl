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

    Int input9
    Int input10
    Int input11
    Int input12
    Int input13
    Int input14
    Int input15
    Int input16

    Boolean input17
    Boolean input18
    Boolean input19
    Boolean input20
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

    Int output9 = 1010
    Int output10 = 1010
    Int output11 = 1010
    Int output12 = 1010
    Int output13 = 1010
    Int output14 = 1010
    Int output15 = 1010
    Int output16 = 1010

    Boolean output17 = false
    Boolean output18 = false
    Boolean output19 = false
    Boolean output20 = false
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
  output {
  }
}

