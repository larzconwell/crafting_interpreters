let rec loop () =
  let str = Readline.readline ~prompt:"> " () in
  match str with
  | None -> ()
  | Some "exit" -> ()
  | Some str ->
    print_endline str;
    loop ()

let () =
  Readline.init ~program_name:"camlox" ();
  loop ()
