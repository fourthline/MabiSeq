/*
 * Copyright (C) 2013 たんらる
 */

package fourthline.mmlTools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import fourthline.mmlTools.core.MMLTicks;
import fourthline.mmlTools.parser.MMLEventParser;


/**
 * 1行のMMLデータを扱います.
 */
public class MMLEventList {

	private List<MMLNoteEvent>   noteList   = new ArrayList<MMLNoteEvent>();
	private List<MMLTempoEvent>  tempoList;

	/**
	 * 
	 * @param mml
	 */
	public MMLEventList(String mml) {
		this(mml, null);
	}

	public MMLEventList(String mml, List<MMLTempoEvent> globalTempoList) {
		if (globalTempoList != null) {
			tempoList = globalTempoList;
		} else {
			tempoList = new ArrayList<MMLTempoEvent>();
		}

		parseMML(mml);
	}

	private void parseMML(String mml) {
		MMLEventParser parser = new MMLEventParser(mml);

		while (parser.hasNext()) {
			MMLEvent event = parser.next();

			if (event instanceof MMLTempoEvent) {
				((MMLTempoEvent) event).appendToListElement(tempoList);
			} else if (event instanceof MMLNoteEvent) {
				if (((MMLNoteEvent) event).getNote() >= 0) {
					noteList.add((MMLNoteEvent) event);
				}
			}
		}
	}

	public void setGlobalTempoList(List<MMLTempoEvent> globalTempoList) {
		tempoList = globalTempoList;
	}

	public List<MMLTempoEvent> getGlobalTempoList() {
		return tempoList;
	}

	public long getTickLength() {
		if (noteList.size() > 0) {
			int lastIndex = noteList.size() - 1;
			MMLNoteEvent lastNote = noteList.get( lastIndex );

			return lastNote.getEndTick();
		} else {
			return 0;
		}
	}

	public List<MMLNoteEvent> getMMLNoteEventList() {
		return noteList;
	}

	private int convertVelocityMML2Midi(int mml_velocity) {
		return (mml_velocity * 8);
	}
	private int convertNoteMML2Midi(int mml_note) {
		return (mml_note + 12);
	}

	private Object nextEvent(Iterator<? extends MMLEvent> iterator) {
		if (iterator.hasNext()) {
			return iterator.next();
		} else {
			return null;
		}
	}

	private static final int INITIAL_VOLUMN = 8;
	public void convertMidiTrack(Track track, int channel) throws InvalidMidiDataException {
		int volumn = INITIAL_VOLUMN;

		// Noteイベントの変換
		for (MMLNoteEvent noteEvent : noteList) {
			int note = noteEvent.getNote();
			int tick = noteEvent.getTick();
			int tickOffset = noteEvent.getTickOffset();
			int endTickOffset = tickOffset + tick - 1;

			// ボリュームの変更
			if (noteEvent.getVelocity() >= 0) {
				volumn = noteEvent.getVelocity();
			}

			// ON イベント作成
			MidiMessage message1 = new ShortMessage(ShortMessage.NOTE_ON, 
					channel,
					convertNoteMML2Midi(note), 
					convertVelocityMML2Midi(volumn));
			track.add(new MidiEvent(message1, tickOffset));

			// Off イベント作成
			MidiMessage message2 = new ShortMessage(ShortMessage.NOTE_OFF,
					channel, 
					convertNoteMML2Midi(note),
					0);
			track.add(new MidiEvent(message2, endTickOffset));
		}
	}


	/**
	 * 指定したtickOffset位置にあるNoteEventを検索します.
	 * @param tickOffset
	 * @return 見つからなかった場合は、nullを返します.
	 */
	public MMLNoteEvent searchOnTickOffset(long tickOffset) {
		for (int i = 0; i < noteList.size(); i++) {
			MMLNoteEvent noteEvent = noteList.get(i);
			if (noteEvent.getTickOffset() <= tickOffset) {
				if (tickOffset <= noteEvent.getEndTick()) {
					return noteEvent;
				}
			} else {
				break;
			}
		}

		return null;
	}

	/**
	 * ノートイベントを追加します.
	 * TODO: MMLNoteEvent のメソッドのほうがいいかな？Listを引数として渡す.
	 * @param addNoteEvent
	 */
	public void addMMLNoteEvent(MMLNoteEvent addNoteEvent) {
		int i;
		if ((addNoteEvent.getNote() <= 0) || (addNoteEvent.getTick() <= 0)) {
			return;
		}

		// 追加したノートイベントに重なる前のノートを調節します.
		for (i = 0; i < noteList.size(); i++) {
			MMLNoteEvent noteEvent = noteList.get(i);
			int tickOverlap = noteEvent.getEndTick() - addNoteEvent.getTickOffset();
			if (addNoteEvent.getTickOffset() < noteEvent.getTickOffset()) {
				break;
			}
			if (tickOverlap >= 0) {
				// 追加するノートに音が重なっている.
				int tick = noteEvent.getTick() - tickOverlap;
				if (tick == 0) {
					noteList.remove(i);
					break;
				} else {
					noteEvent.setTick(tick);
					i++;
					break;
				}
			}
		}

		// ノートイベントを追加します.
		noteList.add(i++, addNoteEvent);

		// 追加したノートイベントに重なっている後続のノートを削除します.
		for ( ; i < noteList.size(); ) {
			MMLNoteEvent noteEvent = noteList.get(i);
			int tickOverlap = addNoteEvent.getEndTick() - noteEvent.getTickOffset();

			if (tickOverlap > 0) {
				noteList.remove(i);
			} else {
				break;
			}
		}
	}

	/**
	 * 指定のMMLeventを削除する.
	 * 最後尾はtrim.
	 * @param deleteItem
	 */
	public void deleteMMLEvent(MMLEvent deleteItem) {
		noteList.remove(deleteItem);
	}

	public String toMMLString() {
		return toMMLString(false, 0);
	}

	public String toMMLString(boolean withTempo) {
		return toMMLString(withTempo, 0);
	}

	private MMLNoteEvent insertTempoMML(StringBuilder sb, MMLNoteEvent prevNoteEvent, MMLTempoEvent tempoEvent, int volumn) {
		// rrrT***N の処理
		if (prevNoteEvent.getEndTick() != tempoEvent.getTickOffset()) {
			int tickLength = tempoEvent.getTickOffset() - prevNoteEvent.getEndTick();
			int tickOffset = prevNoteEvent.getEndTick();
			int note = prevNoteEvent.getNote();
			prevNoteEvent = new MMLNoteEvent(note, tickLength, tickOffset);
			MMLTicks ticks = new MMLTicks("c", tickLength, false);
			sb.append("v0").append(ticks.toString()).append("v"+volumn);
		}
		sb.append(tempoEvent.toMMLString());

		return prevNoteEvent;
	}

	/**
	 * テンポ出力を行うかどうかを指定してMML文字列を作成する.
	 * TODO: 長いなぁ。
	 * @param withTempo trueを指定すると、tempo指定を含むMMLを返します.
	 * @param totalTick 最大tick長. これに満たない場合は、末尾を休符分で埋めます.
	 * @return
	 */
	public String toMMLString(boolean withTempo, int totalTick) {
		//　テンポ
		Iterator<MMLTempoEvent> tempoIterator = null;
		MMLTempoEvent tempoEvent = null;
		tempoIterator = tempoList.iterator();
		tempoEvent = (MMLTempoEvent) nextEvent(tempoIterator);

		//　ボリューム
		int volumn = INITIAL_VOLUMN;

		StringBuilder sb = new StringBuilder();
		int noteCount = noteList.size();

		// initial note: octave 4, tick 0, offset 0
		MMLNoteEvent noteEvent = new MMLNoteEvent(12*4, 0, 0);
		MMLNoteEvent prevNoteEvent = noteEvent;
		for (int i = 0; i < noteCount; i++) {
			noteEvent = noteList.get(i);

			// テンポのMML挿入判定
			if ( (tempoEvent != null) && (tempoEvent.getTickOffset() <= noteEvent.getTickOffset()) ) {
				if (withTempo) {
					// tempo挿入 (rrrT***N の処理)
					prevNoteEvent = insertTempoMML(sb, prevNoteEvent, tempoEvent, volumn);
				}
				tempoEvent = (MMLTempoEvent) nextEvent(tempoIterator);
			}

			// 音量のMML挿入判定
			int noteVelocity = noteEvent.getVelocity();
			if ( (noteVelocity >= 0) && (noteVelocity != volumn) ) {
				volumn = noteVelocity;
				sb.append(noteEvent.getVelocityString());
			}

			// endTickOffsetがTempoを跨いでいたら、そこで切る.
			if ( (tempoEvent != null) && (noteEvent.getTickOffset() < tempoEvent.getTickOffset()) && (tempoEvent.getTickOffset() < noteEvent.getEndTick()) ) {
				int tick = tempoEvent.getTickOffset() - noteEvent.getTickOffset();
				// TODO: 内部データ上も切ること.
				noteEvent.setTick(tick);
				//				noteEvent = new MMLNoteEvent(noteEvent.getNote(), tick, noteEvent.getTickOffset());
			}
			sb.append( noteEvent.toMMLString(prevNoteEvent) );
			prevNoteEvent = noteEvent;
		}

		// テンポがまだ残っていれば、その分をつなげる.
		if ( (withTempo) && (tempoEvent != null) && (noteEvent != null) ) {
			int endTick = noteEvent.getEndTick();
			int tickOffset = tempoEvent.getTickOffset();
			int tick = tickOffset - endTick;
			if (tick > 0) {
				sb.append("v0");
				sb.append( new MMLTicks("c", tick, false).toString() );
			}
			sb.append(tempoEvent.toMMLString());
			noteEvent = new MMLNoteEvent(noteEvent.getNote(), noteEvent.getTick() + tick, noteEvent.getTickOffset());
		}

		return sb.toString();
	}

	@Override
	public String toString() {
		return tempoList.toString() + noteList.toString();
	}
}