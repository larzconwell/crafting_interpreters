let rec loop () =
  let str = Readline.readline ~prompt:"> " () in
  match str with
  | None -> ()
  | Some "exit" -> ()
  | Some "" -> loop ()
  | Some str ->
    Readline.add_history str;
    print_endline str;
    loop ()

let () =
  Readline.init
    ~program_name:"camlox"
    ~history_file:".repl_history"
    ();
  loop ()
