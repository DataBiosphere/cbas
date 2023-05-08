workflow test_all_engine_functions {

    call run_read_string
    call run_read_lines
    call run_read_int
    call run_read_float
    call run_read_boolean
    call run_read_json

    output {
        String string_out = run_read_string.out
        Array[String] lines_out = run_read_lines.out
        Int int_out = run_read_int.out
        Float float_out = run_read_float.out
        Boolean boolean_out = run_read_boolean.out
        String json_out = run_read_json.out.hello
    }
}

task run_read_string {
    command {
        echo "hello world" > myfile.txt
    }
    runtime {
        docker: "ubuntu:latest"
    }
    output {
        String out = read_string("myfile.txt")
    }
}

task run_read_lines {
    command {
        echo "hello world 1" > myfile.txt
        echo "hello world 2" >> myfile.txt
        echo "hello world 3" >> myfile.txt
    }
    runtime {
        docker: "ubuntu:latest"
    }
    output {
        Array[String] out = read_lines("myfile.txt")
    }
}

task run_read_int {
    command {
        echo "42" > myfile.txt
    }
    runtime {
        docker: "ubuntu:latest"
    }
    output {
        Int out = read_int("myfile.txt")
    }
}

task run_read_float {
    command {
        echo "42.0" > myfile.txt
    }
    runtime {
        docker: "ubuntu:latest"
    }
    output {
        Float out = read_float("myfile.txt")
    }
}

task run_read_boolean {
    command {
        echo "true" > myfile.txt
    }
    runtime {
        docker: "ubuntu:latest"
    }
    output {
        Boolean out = read_boolean("myfile.txt")
    }
}

task run_read_json {
    command <<<
        echo '{"hello": "world"}' > myfile.txt
    >>>
    runtime {
        docker: "ubuntu:latest"
    }
    output {
        Object out = read_json("myfile.txt")
    }
}
