package plugins.mitiv;


import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.plugin.PluginLauncher;
import icy.plugin.PluginLoader;
import icy.sequence.Sequence;
import icy.system.thread.ThreadUtil;
import plugins.adufour.ezplug.EzLabel;
import plugins.adufour.ezplug.EzPlug;

public class IcyJulia extends EzPlug  {

    EzLabel status;
    boolean run = false;
    static final int PORT = 10001;
    static final int BYTE   = 0;
    static final int SHORT  = 1;
    static final int INT    = 2;
    static final int FLOAT  = 3;
    static final int DOUBLE = 4;

    byte[] arrayByte = null;


    private void reset() {
        arrayByte = null;
    }

    private void convertDataToIcy(String title,int[] dims, int rank, int type) {
        if (rank<2) {
            System.err.println("Data of rank "+rank+" not supported");
            return;
        }

        if (rank>4) {
            System.err.println("Data of rank "+rank+" not supported");
            return;
        }
        Sequence seq = new Sequence(title);
        switch (type) {
            case BYTE:
                ByteBuffer bytebuffer = ByteBuffer.wrap(arrayByte);
                for (int i = 0; i < dims[3]; i++) {
                    for (int j = 0; j < dims[2]; j++) {
                        int size = dims[0]*dims[1];
                        byte[] tmp = new byte[size];
                        bytebuffer.get(tmp, 0, size);
                        seq.setImage(i,j, new IcyBufferedImage(dims[0], dims[1], tmp));
                    }
                }
                break;
            case SHORT:
                ShortBuffer shortbuffer = ByteBuffer.wrap(arrayByte).asShortBuffer();
                for (int i = 0; i < dims[3]; i++) {
                    for (int j = 0; j < dims[2]; j++) {
                        int size = dims[0]*dims[1];
                        short[] tmp = new short[size];
                        shortbuffer.get(tmp, 0, size);
                        seq.setImage(i,j, new IcyBufferedImage(dims[0], dims[1], tmp));
                    }
                }
                break;
            case INT:
                IntBuffer intbuffer = ByteBuffer.wrap(arrayByte).asIntBuffer();
                for (int i = 0; i < dims[3]; i++) {
                    for (int j = 0; j < dims[2]; j++) {
                        int size = dims[0]*dims[1];
                        int[] tmp = new int[size];
                        intbuffer.get(tmp, 0, size);
                        seq.setImage(i,j, new IcyBufferedImage(dims[0], dims[1], tmp));
                    }
                }
                break;
            case FLOAT:
                FloatBuffer floatbuffer = ByteBuffer.wrap(arrayByte).asFloatBuffer();
                for (int i = 0; i < dims[3]; i++) {
                    for (int j = 0; j < dims[2]; j++) {
                        int size = dims[0]*dims[1];
                        float[] tmp = new float[size];
                        floatbuffer.get(tmp, 0, size);
                        seq.setImage(i,j, new IcyBufferedImage(dims[0], dims[1], tmp));
                    }
                }
                break;
            case DOUBLE:
                DoubleBuffer doublebuffer = ByteBuffer.wrap(arrayByte).asDoubleBuffer();
                for (int i = 0; i < dims[3]; i++) {
                    for (int j = 0; j < dims[2]; j++) {
                        int size = dims[0]*dims[1];
                        double[] tmp = new double[size];
                        if (doublebuffer.remaining()<size) {
                            break;
                        }
                        doublebuffer.get(tmp, 0, size);
                        seq.setImage(i,j, new IcyBufferedImage(dims[0], dims[1], tmp));
                    }
                }
                break;
            default:
                break;
        }
        addSequence(seq);
    }

    @Override
    public void clean() {
        stopExecution();
    }

    @Override
    protected void execute() {
        if (run){
        System.out.println("Server already running");
        return;
        }
        ThreadUtil.bgRun(() -> {
            run = true;
            status.setText("Run");
            ServerSocket serverSocket =null;
            try {
                serverSocket = new ServerSocket(PORT, 0, InetAddress.getByName(null));
                serverSocket.setReuseAddress(true);
                while (run) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        // Receiving dimensions
                        //System.out.println("Client arrived");
                        String detail = in.readLine();

                        in.close();
                        if (detail.compareTo("Closing") == 0) {
                            continue;
                        }
                        clientSocket.close();
                        //splitting before name and data info
                        String[] tmpDataTitle = detail.split("#");
                        System.out.println(Arrays.toString(tmpDataTitle));
                        // Extracting informations
                        String[] details = tmpDataTitle[0].split("x");
                        int type = Integer.parseInt(details[0]);
                        int count = 1;
                        int rank = 0;
                        int dims[] =  new int[]{1,1,1,1};
                        for (int i = 1; i < details.length; i++) {
                            int tmp = Integer.parseInt(details[i]);
                            count *= tmp;
                            dims[i-1] = tmp;
                            rank++;
                        }
                        //System.out.println("TYPE: "+type+" COUNT "+count+" RANK "+rank);
                        int sizebyte =0;
                        // Creating data
                        switch (type) {
                            case BYTE:
                                sizebyte = count  ;
                                break;
                            case SHORT:
                                sizebyte = count * 2 ;
                                break;
                            case INT:
                                sizebyte = count * 4 ;
                                break;
                            case FLOAT:
                                sizebyte = count * 4 ;
                                break;
                            case DOUBLE:
                                sizebyte = count * 8 ;
                                break;
                            default:
                                break;
                        }
                        // Receiving Data
                        clientSocket = serverSocket.accept();
                        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        DataInputStream data = new DataInputStream(clientSocket.getInputStream());

                        arrayByte = new byte[sizebyte];

                        data.readFully(arrayByte, 0, sizebyte);

                        convertDataToIcy(tmpDataTitle[1], dims, rank, type);
                        in.close();
                        clientSocket.close();
                        reset();
                        arrayByte = null;
                    } catch (IOException e) {
                        System.err.println("Error while handling client request: " + e.getMessage());
                    }
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                status.setText("Failed see Logs");
                e.printStackTrace();
            } finally {
                try {
                    if (serverSocket != null) {
                        serverSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void initialize() {
        //PluginRepositoryLoader.waitLoaded();
        status = new EzLabel("Stopped");
        getUI().setParametersIOVisible(false);
        getUI().clickRun();

    }

    @Override
    public void stopExecution(){
        run = false;
        try {
            Socket kkSocket = new Socket("localhost", PORT);
            BufferedOutputStream out = new BufferedOutputStream(kkSocket.getOutputStream());
            String end = "Closing";
            out.write(end.getBytes());
            out.close();
            kkSocket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        status.setText("Stopped");
    }
    public static void main( final String[] args )
    {
        // Launch the application.
        Icy.main( args );

        /*
         * Programmatically launch a plugin, as if the user had clicked its
         * button.
         */
        PluginLauncher.start( PluginLoader.getPlugin( IcyJulia.class.getName() ) );
    }
}
