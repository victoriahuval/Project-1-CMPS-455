public class Main {
    public static void main(String[] args) throws InterruptedException {
        if(args.length < 2){
            System.out.println("Error: Missing arguments.");
            return;
        }
        if(!args[0].equals("-A")){
            System.out.println("Error: Unknown argument. '" + args[0] + "'. ");
            return;
        }
        int t;
        try{
            t = Integer.parseInt(args[1]);
        }catch(NumberFormatException e){
                System.out.println("Error: '" + args[1] + "' is not valid number.");
                return;
            }
        if(t == 1 ){
            DiningPhilosopher.run();
        }
        else if (t == 2 ){
            ReaderCoordinator.run();
        }
        else{
            System.out.println("Error. Invalid task. Try again please.");
        }
    }
}
