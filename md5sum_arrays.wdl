version 1.0

workflow md5sum_workflow {
    input {
        Array[File] files
    }
    scatter (i in range(15)) {
        call md5sum_arrays_task {
            input: f = files
        }
    }
    output {
        Array[String] checksums = flatten(md5sum_arrays_task.checksums)
    }
}

task md5sum_arrays_task {
    input {
      Array[File] f
    }
    command {
        md5sum ~{sep=' ' f}
    }
    runtime {
        docker: "ubuntu:latest"
    }

    output {
        Array[String] checksums = read_lines(stdout())
    }
}

