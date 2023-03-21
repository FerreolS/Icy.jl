package plugins.mitiv;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;

import javax.swing.JLabel;

import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import plugins.adufour.ezplug.EzLabel;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzStoppable;

public class IcyJulia extends EzPlug implements EzStoppable {

    EzLabel status;
    JLabel url2;
    boolean run = true;
    static final int PORT = 10001;
    static final int INT    = 0;
    static final int LONG   = 1;
    static final int FLOAT  = 2;
    static final int DOUBLE = 3;

    int[] arrayInt = null;
    long[] arrayLong = null;
    float[] arrayFloat = null;
    double[] arrayDouble = null;

    private void reset() {
        arrayInt = null;
        arrayLong = null;
        arrayFloat = null;
        arrayDouble = null;
    }

    private void convertDataToIcy(String title,int[] dims, int rank, int type) {
        if (rank > 1 || rank < 5) {
            Sequence seq = new Sequence(title);
            for (int i = 0; i < dims[3]; i++) {
                for (int j = 0; j < dims[2]; j++) {
                    int size = dims[0]*dims[1];
                    switch (type) {
                    case INT:
                        int[] tmpInt = new int[size];
                        for (int ii = 0; ii < size; ii++) {
                            tmpInt[ii] = arrayInt[i*dims[2]*size+j*size+ii];
                        }
                        seq.setImage(i,j, new IcyBufferedImage(dims[0], dims[1], tmpInt));
                        break;
                    case LONG:
                        long[] tmpLong = new long[size];
                        for (int ii = 0; ii < size; ii++) {
                            tmpLong[ii] = arrayLong[i*dims[2]*size+j*size+ii];
                        }
                        seq.setImage(i,j, new IcyBufferedImage(dims[0], dims[1], tmpLong));
                        break;
                    case FLOAT:
                        float[] tmpFloat = new float[size];
                        for (int ii = 0; ii < size; ii++) {
                            tmpFloat[ii] = arrayFloat[i*dims[2]*size+j*size+ii];
                        }
                        seq.setImage(i,j, new IcyBufferedImage(dims[0], dims[1], tmpFloat));
                        break;
                    case DOUBLE:
                        double[] tmpDouble = new double[size];
                        for (int ii = 0; ii < size; ii++) {
                            tmpDouble[ii] = arrayDouble[i*dims[2]*size+j*size+ii];
                        }
                        seq.setImage(i,j, new IcyBufferedImage(dims[0], dims[1], tmpDouble));
                        break;
                    default:
                        break;
                    }
                }
            }
            addSequence(seq);
        } else {
            System.err.println("Data of rank "+rank+" not supported");
        }
    }

    @Override
    public void clean() {

    }

    @Override
    protected void execute() {
        run = true;
        status.setText("Launching");
        ServerSocket a = null;
        try {
            a = new ServerSocket(PORT, 0, InetAddress.getByName(null));
            a.setReuseAddress(true);
            while(run) {
                Socket clientSocket = a.accept();
                // Receiving dimensions
                System.out.println("Client arrived");
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
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
                System.out.println("TYPE: "+type+" COUNT "+count+" RANK "+rank);
                // Creating data
                switch (type) {
                case INT:
                    arrayInt = new int[count];
                    break;
                case LONG:
                    arrayLong = new long[count];
                    break;
                case FLOAT:
                    arrayFloat = new float[count];
                    break;
                case DOUBLE:
                    arrayDouble = new double[count];
                    break;
                default:
                    break;
                }
                // Receiving Data
                clientSocket = a.accept();
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                DataInputStream data = new DataInputStream(clientSocket.getInputStream());

                for (int i = 0; i < count; i++) {
                    switch (type) {
                    case INT:
                        arrayInt[i] = data.readInt();
                        break;
                    case LONG:
                        arrayLong[i] = data.readLong();
                        break;
                    case FLOAT:
                        arrayFloat[i] =  data.readFloat();
                        break;
                    case DOUBLE:
                        arrayDouble[i] = data.readDouble();
                        break;
                    default:
                        System.out.println("Invalide data type sent");
                        continue;
                    }
                }
                convertDataToIcy(tmpDataTitle[1], dims, rank, type);
                in.close();
                clientSocket.close();
                reset();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            status.setText("Failed see Logs");
            e.printStackTrace();
        } finally {
            try {
                if (a != null) {
                    a.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    protected void initialize() {
        status = new EzLabel("Stopped");
        url2 = new JLabel("<html> <a href=\"https://github.com/Lightjohn\">Julia Code</a> </html>");
        url2.setCursor(new Cursor(Cursor.HAND_CURSOR));
        url2.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                    try {
                            Desktop.getDesktop().browse(new URI("https://github.com/Lightjohn"));
                    } catch (URISyntaxException e1) {
                        System.err.println(e1);
                    } catch ( IOException e2) {
                        System.err.println(e2);
                    }
            }
        });
        addComponent(url2);
        addEzComponent(status);
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

}
