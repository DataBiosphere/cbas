version 1.0

workflow size_of_input {
    input {
      File f
    }
    Float s = size(f, "GiB")

    output {
        Float s_out = s
    }
}



