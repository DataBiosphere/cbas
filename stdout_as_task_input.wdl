version 1.0

workflow test_all_engine_functions {
    call create_stdout

    call use_stdout { input: my_stdout = create_stdout.my_stdout }

    output {
        File my_processed_stdout = use_stdout.my_processed_stdout
    }
}

task create_stdout {
    command {
        echo "Hello World!"
    }
    runtime {
        docker: "ubuntu:latest"
    }
    output {
        File my_stdout = stdout()
    }
}

task use_stdout {
    input {
      File my_stdout
    }
    command {
        cat ${my_stdout} | sed 's/Hello/Goodbye/'
    }
    runtime {
      docker: "ubuntu:latest"
    }
    output {
        File my_processed_stdout = stdout()
    }
}

