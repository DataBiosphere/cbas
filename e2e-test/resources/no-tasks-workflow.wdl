version 1.0

workflow no_tasks {
  input {
    String name
    String id
  }

  output {
    Int team = "Guardians of the Galaxy"
    Int rank = "Captain"
  }
}
