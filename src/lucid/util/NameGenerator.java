package lucid.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class NameGenerator {
    public static String getName() {
        try{
            Random r = new Random();
            BufferedReader in = new BufferedReader(new FileReader(new File("names.txt")));
            ArrayList<String> names = new ArrayList<String>();
            String name = null;
            while((name = in.readLine()) != null) {
                names.add(name);
            }
            in.close();
            int index = r.nextInt(names.size());
            return names.get(index);
        }catch(IOException e){
            System.out.println("Failed to open names.txt, returning null");
        }
        return null;
    }
}
