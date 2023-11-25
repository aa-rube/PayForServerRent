package app.command_executor;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Component
public class RemoteCommandExecutor {

    public static synchronized void executeCommand(String command) throws Exception {

        JSch jsch = new JSch();
        Session session = jsch.getSession("your-username", "your-host", 22); // replace with your actual SSH credentials
        session.setPassword("your-password"); // replace with your actual password
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        try {
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);

            InputStream inputStream = channel.getInputStream();
            InputStream errorStream = channel.getErrStream();
            BufferedReader outputReader = new BufferedReader(new InputStreamReader(inputStream));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));

            channel.connect();

            // Read and print the output
            String line;
            while ((line = outputReader.readLine()) != null) {
                System.out.println("Output: " + line);
            }

            // Read and print the error, if any
            while ((line = errorReader.readLine()) != null) {
                System.out.println("Error: " + line);
            }

            channel.disconnect();
        } finally {
            session.disconnect();
        }
    }

    public static String executeLocalCommand(String command) throws IOException {
        try {
            Process process = Runtime.getRuntime().exec(command);

            // Read the output
            InputStream inputStream = process.getInputStream();
            BufferedReader outputReader = new BufferedReader(new InputStreamReader(inputStream));

            // Read and print the output
            StringBuilder builder = new StringBuilder();
            builder.append("Output:\n");
            String line;
            while ((line = outputReader.readLine()) != null) {
                builder.append(line);
            }

            // Wait for the process to finish
            int exitCode;
            try {
                exitCode = process.waitFor();
            } catch (InterruptedException e) {
                return "Error waiting for the process to finish.\n" + e;
            }

            if (exitCode != 0) {
                return "Command exited with non-zero status: " + exitCode;
            }

            return builder.toString();
        } catch (IOException e) {
            throw new IOException("Error executing command: " + command, e);
        }
    }


}