version 1.0

workflow no_tasks {
  input {
    String name
    String id
  }

  output {
    String team = "Guardians of the Galaxy"
    String rank = "Captain"
  }
}
