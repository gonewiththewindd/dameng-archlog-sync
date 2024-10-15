package org.gone.dameng.datasync.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SshUtils {

    public static String execRemoteCommand(String host, int port, String username, String password, String command, int timeoutSeconds) {

        SshClient client = SshClient.setUpDefaultClient();
        client.start();

        try (ClientSession session = client.connect(username, host, port)
                .verify(timeoutSeconds, TimeUnit.SECONDS).getSession()) {
            session.addPasswordIdentity(password);
            session.auth().verify(timeoutSeconds, TimeUnit.SECONDS);

            try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                 ChannelExec execChannel = session.createExecChannel(command)) {
                execChannel.setOut(out);
                execChannel.open().verify(TimeUnit.SECONDS.toMillis(timeoutSeconds));

                execChannel.waitFor(Arrays.asList(ClientChannelEvent.CLOSED), timeoutSeconds * 1000);
                String result = out.toString("GBK");
                log.info("[Remote-Shell]command '{}' execute result:\n{}", command, result);
                return result;
            }
        } catch (Exception e) {
            log.error("[Remote-Shell]failed to check database instance status.", e);
            throw new RuntimeException(e);
        } finally {
            client.stop();
        }
    }


}
