
import javax.sound.midi.*;
        import javax.swing.*;
        import java.awt.event.KeyEvent;
        import java.awt.event.KeyListener;
        import java.io.File;
        import java.io.IOException;
        import java.util.ArrayList;
        import java.util.List;

public class KeyboardMixer extends JFrame {
    private boolean isRecording = false;
    private final List<MidiEvent> recordedEvents = new ArrayList<>();

    public KeyboardMixer() {
        super("Keyboard Mixer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initializeUI();
        initializeMidi();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initializeUI() {
        JTextArea textArea = new JTextArea();
        add(textArea);

        addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                int note = e.getKeyCode() % 128; // Map each key to a MIDI note

                if (isRecording) {
                    long timestamp = System.currentTimeMillis();
                    recordedEvents.add(createNoteOnEvent(note, timestamp));
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int note = e.getKeyCode() % 128;

                if (isRecording) {
                    long timestamp = System.currentTimeMillis();
                    recordedEvents.add(createNoteOffEvent(note, timestamp));
                }
            }
        });

        JButton recordButton = new JButton("Record");
        JButton stopButton = new JButton("Stop");
        JButton playButton = new JButton("Play");

        recordButton.addActionListener(e -> {
            isRecording = true;
            recordedEvents.clear();
            textArea.setText("Recording...");
        });

        stopButton.addActionListener(e -> {
            isRecording = false;
            saveRecordedEvents();
            textArea.setText("Recording stopped.");
        });

        playButton.addActionListener(e -> {
            playRecordedEvents();
            textArea.setText("Playing recorded events...");
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(recordButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(playButton);

        add(buttonPanel);
    }

    private void initializeMidi() {
        try {
            Synthesizer synth = MidiSystem.getSynthesizer();
            synth.open();
            Receiver receiver = synth.getReceiver();
            Transmitter transmitter = synth.getTransmitter();
            transmitter.setReceiver(receiver);
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        }
    }

    private MidiEvent createNoteOnEvent(int note, long timestamp) {
        try {
            return new MidiEvent(new ShortMessage(ShortMessage.NOTE_ON, 0, note, 64), timestamp);
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    private MidiEvent createNoteOffEvent(int note, long timestamp) {
        try {
            return new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, 0, note, 64), timestamp);
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveRecordedEvents() {
        try {
            Sequence sequence = new Sequence(Sequence.PPQ, 24);

            Track track = sequence.createTrack();
            for (MidiEvent event : recordedEvents) {
                track.add(event);
            }

            MidiSystem.write(sequence, 1, new File("recorded_music.mid"));
        } catch (InvalidMidiDataException | IOException e) {
            e.printStackTrace();
        }
    }

    private void playRecordedEvents() {
        try {
            Sequencer sequencer = MidiSystem.getSequencer();
            sequencer.open();

            Sequence sequence = new Sequence(Sequence.PPQ, 24);
            Track track = sequence.createTrack();
            for (MidiEvent event : recordedEvents) {
                track.add(event);
            }

            sequencer.setSequence(sequence);
            sequencer.start();
            while (sequencer.isRunning()) {
                // Wait for the playback to finish
            }
            sequencer.close();
        } catch (MidiUnavailableException | InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(KeyboardMixer::new);
    }
}
