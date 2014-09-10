import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Created by WITAN on 9/10/14.
 */

public class ReverseWordList implements MigratableProcess{
    private enum Phase {
        Reading, Trim, CountLength, Reverse, Write
    }
    private static final long serialVersionUID = -123456789;
    private TransactionalFileInputStream inFile;
    private TransactionalFileOutputStream outFile;
    private String line;
    private String Reversed;
    private Phase phase;
    private int end = 0;

    private volatile boolean suspending = false;

    public ReverseWordList(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("usage: GrepProcess <inputFile> <outputFile>");
            throw new Exception("Invalid Arguments");
        }

        inFile = new TransactionalFileInputStream(args[1]);
        outFile = new TransactionalFileOutputStream(args[2], false);
        this.phase = Phase.Reading;
        this.Reversed = "";
    }

    public void run() {
        System.out.println("Slave Node task start running");
        PrintStream out = new PrintStream(outFile);
        DataInputStream in = new DataInputStream(inFile);

        try {
            while (!suspending) {

                switch (this.phase) {
                    case Reading:
                        this.line = in.readLine();
                        System.out.println(this.line);
                        if (this.line == null){
                            throw new EOFException();
                        }
                        this.phase = Phase.Trim;
                        break;
                    case Trim:
                        this.line = this.line.trim();
                        if (this.line.equals(""))
                        {
                            this.phase = Phase.Write;
                        }
                        this.phase = Phase.CountLength;
                        break;
                    case CountLength:
                        this.line = " " + this.line;
                        this.end = this.line.length();
                        this.phase = Phase.Reverse;
                        break;
                    case Reverse:
                        for(int i=this.end - 1; i>= 0; i--)
                        {
                            if(this.line.charAt(i)==' ')
                            {
                                this.Reversed=this.Reversed + this.line.substring(i, this.end);
//                                System.out.println(this.Reversed);
                                if(i!=0)
                                {
                                    while(this.line.charAt(i-1)==' ')
                                    {
                                        i=i-1;
                                    }
                                    this.end=i;
                                }
                            }
                        }
                        this.phase = Phase.Write;
                        break;
                    case Write:
                        out.println(this.Reversed);
                        this.Reversed = "";
                        this.phase = Phase.Reading;
                        break;
                    default:
                        break;
                }

                // Make grep take longer so that we don't require extremely
                // large files for interesting results
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // ignore it
                }
            }
        } catch (EOFException e) {
            // End of File
        } catch (IOException e) {
            System.out.println("GrepProcess: Error: " + e);
        }
        out.close();
        suspending = false;
    }

    public void suspend() {
        suspending = true;
        while (suspending);
    }
}