package com.nctigba.observability.log.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.EnumSet;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClientFactory;

public class SshSession implements AutoCloseable {
	private static final int SESSION_TIMEOUT = 10000;
	private static final int CHANNEL_TIMEOUT = 50000;

	public enum command {
		ARCH("arch"),
		CD("cd {0}"),
		LS("ls {0}"),
		STAT("stat {0}"),
		WGET("wget {0}"),
		TAR("tar zxf {0}"),
		APPEND_FILE(""),
		CHECK_USER("cat /etc/passwd | awk -F \":\" \"'{print $1}\"|grep {0} | wc -l"),
		CREATE_USER("useradd omm && echo ''{0} ALL=(ALL) ALL'' >> /etc/sudoers"),
		CHANGE_PASSWORD("passwd {1}"),
		PS("ps -ef|grep {0} |grep -v grep |awk ''{print $2}''"),
		KILL("kill -9 {0}");

		private String cmd;

		command(String cmd) {
			this.cmd = cmd;
		}

		public String parse(Object... args) {
			return MessageFormat.format(cmd, args);
		}
	}

	public boolean test(String command) throws IOException {
		try {
			execute(command);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	public String execute(command command) throws IOException {
		return execute(command.cmd, null);
	}

	public String execute(String command) throws IOException {
		return execute(command, null);
	}

	public String execute(String command, OutputStream os) throws IOException {
		var ec = session.createExecChannel(command);
		if (os == null)
			os = new ByteArrayOutputStream();
		ec.setOut(os);
		ec.setErr(os);
		ec.open();
		ec.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), CHANNEL_TIMEOUT);
		if (ec.getExitStatus() != 0)
			throw new RuntimeException();
		return os.toString().trim();
	}

	public synchronized void upload(String source, String target) throws IOException {
		var sf = SftpClientFactory.instance().createSftpFileSystem(session);
		var path = sf.getDefaultDir().resolve(target);
		Files.copy(Paths.get(source), path, StandardCopyOption.REPLACE_EXISTING);
	}

	private static SshClient cli;
	private ClientSession session;

	private static final synchronized SshClient getClient() {
		if (cli == null) {
			cli = SshClient.setUpDefaultClient();
			cli.start();
		}
		return cli;
	}

	private SshSession(String host, Integer port, String username, String password) throws IOException {
		var cf = getClient().connect(username, host, port);
		session = cf.verify().getSession();
		session.addPasswordIdentity(password);
		session.auth().verify(SESSION_TIMEOUT);
	}

	public static SshSession connect(String host, Integer port, String username, String password) throws IOException {
		return new SshSession(host, port, username, password);
	}

	@Override
	public void close() throws IOException {
		session.close();
	}
}