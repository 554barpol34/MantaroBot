package net.kodehawa.mantarobot.commands.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;
import net.kodehawa.mantarobot.data.Data.GuildData;
import net.kodehawa.mantarobot.data.MantaroData;

import java.awt.Color;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kodehawa.mantarobot.utils.SimpleFileDataManager.NEWLINE_PATTERN;

public class AudioCmdUtils {
	public static boolean connectToVoiceChannel(GuildMessageReceivedEvent event) {
		VoiceChannel userChannel = event.getMember().getVoiceState().getChannel();

		if (userChannel == null) {
			event.getChannel().sendMessage("\u274C Please join a voice channel!").queue();
			return false;
		}

		VoiceChannel guildMusicChannel = event.getGuild().getVoiceChannelById(MantaroData.getData().get().guilds.getOrDefault(event.getGuild().getId(), new GuildData()).musicChannel);
		AudioManager audioManager = event.getGuild().getAudioManager();

		if (guildMusicChannel != null) {
			if (!userChannel.equals(guildMusicChannel)) {
				event.getChannel().sendMessage("\u274C I can only play music on channel **" + guildMusicChannel.getName() + "**!").queue();
				return false;
			}

			if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
				audioManager.openAudioConnection(userChannel);
				event.getChannel().sendMessage("\uD83D\uDCE3 Connected to channel **" + userChannel.getName() + "**!").queue();
			}

			return true;
		}

		if (!audioManager.getConnectedChannel().equals(userChannel)) {
			event.getChannel().sendMessage("\u274C I'm already connected on channel **" + audioManager.getConnectedChannel().getName() + "**! (Use the `move` command to move me to another channel)").queue();
			//TODO The ACTUAL ~>move command. (Do this TODO at AudioCmds.java)
			return false;
		}

		if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
			audioManager.openAudioConnection(userChannel);
			event.getChannel().sendMessage("\uD83D\uDCE3 Connected to channel **" + userChannel.getName() + "**!").queue();
		}

		return true;
	}

	public static MessageEmbed embedForQueue(Guild guild, MusicManager musicManager) {
		String toSend = musicManager.getScheduler().getQueueList();
		String[] lines = NEWLINE_PATTERN.split(toSend);

		if (lines.length > 15) {
			toSend = Stream.concat(
				Stream.of(lines).limit(14),
				Stream.of("Showing only first **15** results.")
			).collect(Collectors.joining("\n"));
		}

		long length = musicManager.getScheduler().getQueue().stream().mapToLong(value -> value.getInfo().length).sum();

		EmbedBuilder builder = new EmbedBuilder()
			.setAuthor("Queue for server " + guild.getName(), null, guild.getIconUrl())
			.setColor(Color.CYAN);

		if (!toSend.isEmpty()) {
			builder.setDescription(toSend)
				.addField("Queue runtime", getDurationMinutes(length), true)
				.addField("Total queue size", String.valueOf(musicManager.getScheduler().getQueue().size()), true);
		} else {
			builder.setDescription("Nothing here, just dust.");
		}

		return builder.build();
	}

	public static String getDurationMinutes(long length) {
		return String.format("%d:%02d minutes",
			TimeUnit.MILLISECONDS.toMinutes(length),
			TimeUnit.MILLISECONDS.toSeconds(length) -
				TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(length))
		);
	}

	public static String getDurationMinutes(AudioTrack track) {
		return getDurationMinutes(track.getInfo().length);
	}
}
