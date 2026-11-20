import { loadFont } from '@remotion/google-fonts/Roboto';
import {
  AbsoluteFill,
  Audio,
  Easing,
  Img,
  interpolate,
  OffthreadVideo,
  Sequence,
  spring,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from 'remotion';

const { fontFamily } = loadFont('normal', { weights: ['400', '500', '700', '900'], subsets: ['latin'] });

export const FPS = 60;
const f = (seconds: number) => Math.round(seconds * FPS);

const BRAND = '#05AEFF';
const GREEN = '#4ADE80';
const SRC_W = 1080;
const SRC_H = 2400;
const SCREEN_W = 640;
const SCREEN_H = Math.round((SCREEN_W * SRC_H) / SRC_W);
const BEZEL = 14;
const BOX_W = SCREEN_W + BEZEL * 2;
const BOX_H = SCREEN_H + BEZEL * 2;
const BOX_LEFT = (1080 - BOX_W) / 2;
const PHONE_TOP = 470;
const PX = SCREEN_W / SRC_W; // a pixel of the recording -> a pixel of the phone's screen
const PUSH = f(0.38);
const VOICE_AT = f(0.25);
const END_VOICE_AT = f(0.5);

// A rectangle of the recording, in its 1080x2400 pixels, lifted out of the screen while the screen holds still.
type Card = { x: number; y: number; w: number; h: number; delay: number; radius: number; tilt: number };
type Step =
  // Plays clip seconds [from, to). `push` slides it in over the last frame of the step before, like a navigation.
  | { kind: 'play'; clip: string; from: number; to: number; enter?: string }
  // Holds the frame where the step before stopped, so the cards lifted from it always match the screen.
  | { kind: 'freeze'; seconds: number; fy: number; cards: Card[] }
  // A screen with nothing moving on it, shown as an image; it can be pushed in like a play.
  | { kind: 'still'; img: string; seconds: number; enter?: string };
type Shot = { title: { text: string; accent?: string[] }[]; subtitle?: string; whip: boolean; steps: Step[] };
// What scripts/store-video/locales/<locale>.json holds that the video draws, handed over in props.json by
// elevenlabs.py prepare, so a new locale needs no change here.
export type Locale = { shots: Shot[]; endSeconds: number; endCard: { name: string[]; chips: string[] } };

export type StoreVideoProps = { video: Locale; voiceSeconds: number[]; musicSeconds: number };

type Placed = { step: Step; start: number; dur: number; shot: number; prev?: Placed };
type Timeline = Locale & { placed: Placed[]; shotStarts: number[]; endAt: number; duration: number; whips: number[] };

// The steps of every shot laid end to end, in frames.
export const timelineOf = (locale: Locale): Timeline => {
  const placed: Placed[] = [];
  const shotStarts: number[] = [];
  let at = 0;
  locale.shots.forEach((shot, si) => {
    shotStarts.push(at);
    shot.steps.forEach((step) => {
      const dur = step.kind === 'play' ? f(step.to) - f(step.from) : f(step.seconds);
      placed.push({ step, start: at, dur, shot: si, prev: placed[placed.length - 1] });
      at += dur;
    });
  });
  shotStarts.push(at);
  const whips = locale.shots.map((shot, i) => (shot.whip ? shotStarts[i] : -1)).filter((x) => x >= 0);
  return { ...locale, placed, shotStarts, endAt: at, duration: at + f(locale.endSeconds), whips };
};

const lastFrameOf = (p: Placed): string =>
  p.step.kind === 'play'
    ? `${p.step.clip}_${f(p.step.to) - 1}.png`
    : p.step.kind === 'still'
      ? p.step.img
      : lastFrameOf(p.prev!);

const ease = Easing.bezier(0.65, 0, 0.35, 1);
const clamp = { extrapolateLeft: 'clamp', extrapolateRight: 'clamp' } as const;

// How far the camera has settled flat on a freeze (0..1), and the freeze it is on.
const freezeAt = (tl: Timeline, frame: number) => {
  for (const p of tl.placed) {
    if (p.step.kind !== 'freeze') continue;
    const ramp = f(0.4);
    const b = Math.min(
      interpolate(frame, [p.start - f(0.1), p.start + ramp], [0, 1], { ...clamp, easing: ease }),
      interpolate(frame, [p.start + p.dur - ramp, p.start + p.dur + f(0.1)], [1, 0], { ...clamp, easing: ease }),
    );
    if (b > 0) return { b, p };
  }
  return { b: 0, p: undefined };
};

type Cam = { scale: number; y: number; rx: number; ry: number; fy: number; swing: number; blur: number };
const camAt = (tl: Timeline, frame: number): Cam => {
  const { shotStarts, endAt } = tl;
  const shot = Math.max(0, shotStarts.findIndex((s, i) => frame >= s && frame < (shotStarts[i + 1] ?? Infinity)));
  const side = shot % 2 === 0 ? -1 : 1;
  const turn = interpolate(frame, [shotStarts[shot], shotStarts[shot] + f(0.5)], [shot === 0 ? side : -side, side], {
    ...clamp,
    easing: ease,
  });
  // The opening: the phone rises into place, settling softly.
  const enter = interpolate(frame, [0, f(1.5)], [0, 1], { ...clamp, easing: Easing.bezier(0.16, 1, 0.3, 1) });
  const exit = interpolate(frame, [endAt, endAt + f(0.8)], [0, 1], { ...clamp, easing: Easing.in(Easing.cubic) });
  const drift = Math.sin(frame / 50) * 2;
  let cam: Cam = {
    scale: 1.16 - 0.14 * enter - 0.3 * exit,
    y: (1 - enter) * 260 + exit * 1700 + Math.sin(frame / 45) * 8 * enter,
    rx: 7 + (1 - enter) * 16 + 20 * exit,
    ry: (1 - enter) * -22 + turn * 9 + drift,
    fy: 0.5,
    swing: 0,
    blur: 0,
  };
  // A whip at each shot that cuts to another place: a quick swing with motion blur.
  for (const w of tl.whips) {
    const d = (frame - w) / f(0.2);
    if (Math.abs(d) < 1) {
      cam.swing += (d < 0 ? -1 : 1) * (1 - Math.abs(d)) * 30;
      cam.blur += (1 - Math.abs(d)) * 10;
    }
  }
  const { b, p } = freezeAt(tl, frame);
  if (p && p.step.kind === 'freeze') {
    const mix = (a: number, z: number) => a + (z - a) * b;
    cam = {
      ...cam,
      scale: mix(cam.scale, 1.12),
      y: mix(cam.y, 0),
      rx: mix(cam.rx, 0),
      ry: mix(cam.ry, 0),
      fy: mix(cam.fy, p.step.fy),
      swing: mix(cam.swing, 0),
    };
  }
  return cam;
};

// Where a pixel of the recording lands in the frame, with the camera flat.
const project = (cam: Cam, x: number, y: number) => {
  const ox = BOX_W / 2;
  const oy = cam.fy * BOX_H;
  const bx = BEZEL + x * PX;
  const by = BEZEL + y * PX;
  return { X: BOX_LEFT + ox + (bx - ox) * cam.scale, Y: PHONE_TOP + cam.y + oy + (by - oy) * cam.scale };
};

const liftOf = (frame: number, p: Placed, card: Card, fps: number) => {
  const up = spring({ frame: frame - p.start - f(card.delay), fps, config: { damping: 15, stiffness: 150, mass: 0.7 } });
  const down = interpolate(frame, [p.start + p.dur - f(0.75), p.start + p.dur - f(0.4)], [1, 0], { ...clamp, easing: ease });
  return Math.min(up, down);
};

export const StoreVideo = ({ video, voiceSeconds, musicSeconds }: StoreVideoProps) => {
  const frame = useCurrentFrame();
  const tl = timelineOf(video);
  return (
    <AbsoluteFill style={{ fontFamily, backgroundColor: '#0A1B26', overflow: 'hidden' }}>
      <Backdrop time={frame / FPS} />
      <Phone tl={tl} />
      <Cards tl={tl} />
      {tl.shots.map((shot, i) => (
        <Sequence key={i} from={tl.shotStarts[i]} durationInFrames={tl.shotStarts[i + 1] - tl.shotStarts[i]}>
          <Title shot={shot} total={tl.shotStarts[i + 1] - tl.shotStarts[i]} />
        </Sequence>
      ))}
      <Sequence from={tl.endAt}>
        <EndCard tl={tl} />
      </Sequence>
      <Sound tl={tl} voiceSeconds={voiceSeconds} musicSeconds={musicSeconds} />
    </AbsoluteFill>
  );
};

const Backdrop = ({ time }: { time: number }) => (
  <AbsoluteFill
    style={{
      background: [
        `radial-gradient(900px 900px at ${30 + Math.sin(time * 0.5) * 12}% ${20 + Math.sin(time * 0.3) * 8}%, rgba(5,174,255,0.30), transparent 70%)`,
        `radial-gradient(800px 800px at ${70 + Math.cos(time * 0.4) * 10}% ${78 + Math.cos(time * 0.35) * 6}%, rgba(74,222,128,0.16), transparent 70%)`,
        'linear-gradient(180deg, #0E2433 0%, #08151E 100%)',
      ].join(','),
    }}
  />
);

const Phone = ({ tl }: { tl: Timeline }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const cam = camAt(tl, frame);
  const { p } = freezeAt(tl, frame);
  const lift = p && p.step.kind === 'freeze' ? Math.max(0, ...p.step.cards.map((c) => liftOf(frame, p, c, fps))) : 0;
  return (
    <AbsoluteFill style={{ perspective: 2400, opacity: interpolate(frame, [0, f(0.45)], [0, 1], clamp) }}>
      <div
        style={{
          position: 'absolute',
          left: BOX_LEFT,
          top: PHONE_TOP,
          width: BOX_W,
          height: BOX_H,
          borderRadius: 70,
          background: 'linear-gradient(145deg, #2a3640, #05080b 40%)',
          padding: BEZEL,
          boxShadow: '0 60px 120px rgba(0,0,0,0.55), 0 0 0 2px rgba(255,255,255,0.06) inset',
          transformOrigin: `50% ${cam.fy * 100}%`,
          transform: `translateY(${cam.y}px) rotateX(${cam.rx}deg) rotateY(${cam.ry + cam.swing}deg) scale(${cam.scale})`,
          filter: `blur(${cam.blur}px)`,
        }}
      >
        <div style={{ width: SCREEN_W, height: SCREEN_H, borderRadius: 56, overflow: 'hidden', position: 'relative', background: '#111418' }}>
          {tl.placed.map((pl, i) => (
            <Sequence key={i} from={pl.start} durationInFrames={pl.dur} layout="none">
              <StepView placed={pl} />
            </Sequence>
          ))}
          {/* The phone leaves on the last frame held still: the last clip ends where its step does. */}
          <Sequence from={tl.endAt} layout="none">
            <Img src={staticFile(lastFrameOf(tl.placed[tl.placed.length - 1]))} style={full} />
          </Sequence>
          <div style={{ position: 'absolute', inset: 0, background: 'black', opacity: 0.5 * lift }} />
          <div
            style={{
              position: 'absolute',
              inset: 0,
              background: `linear-gradient(115deg, transparent ${30 + cam.ry}%, rgba(255,255,255,0.06) ${42 + cam.ry}%, transparent ${55 + cam.ry}%)`,
            }}
          />
        </div>
      </div>
    </AbsoluteFill>
  );
};

const full: React.CSSProperties = { position: 'absolute', inset: 0, width: SCREEN_W, height: SCREEN_H };

const StepView = ({ placed: pl }: { placed: Placed }) => {
  const frame = useCurrentFrame(); // relative to the step
  const { step } = pl;
  const content =
    step.kind === 'play' ? (
      <OffthreadVideo src={staticFile(`${step.clip}.mp4`)} startFrom={f(step.from)} muted style={full} />
    ) : (
      <Img src={staticFile(lastFrameOf(step.kind === 'still' ? pl : pl.prev!))} style={full} />
    );
  if (step.kind === 'freeze' || step.enter !== 'push' || !pl.prev) return content;
  const t = interpolate(frame, [0, PUSH], [0, 1], { ...clamp, easing: Easing.bezier(0.2, 0, 0, 1) });
  return (
    <>
      <div style={{ ...full, transform: `translateX(${-t * SCREEN_W * 0.3}px)` }}>
        <Img src={staticFile(lastFrameOf(pl.prev))} style={full} />
        <div style={{ ...full, background: 'black', opacity: 0.45 * t }} />
      </div>
      <div style={{ ...full, transform: `translateX(${(1 - t) * SCREEN_W}px)`, boxShadow: '-20px 0 40px rgba(0,0,0,0.4)' }}>
        {content}
      </div>
    </>
  );
};

const Cards = ({ tl }: { tl: Timeline }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const { p } = freezeAt(tl, frame);
  if (!p || p.step.kind !== 'freeze') return null;
  const cam = camAt(tl, frame);
  const still = staticFile(lastFrameOf(p.prev!));
  return (
    <AbsoluteFill>
      {p.step.cards.map((card, i) => {
        const lift = liftOf(frame, p, card, fps);
        if (lift <= 0.001) return null;
        const { X, Y } = project(cam, card.x, card.y);
        const k = PX * cam.scale;
        return (
          <div
            key={i}
            style={{
              position: 'absolute',
              left: X,
              top: Y,
              width: card.w * k,
              height: card.h * k,
              overflow: 'hidden',
              borderRadius: card.radius * k * (1 + 0.3 * lift),
              transform: `translateY(${-30 * lift}px) scale(${1 + 0.28 * lift}) rotateZ(${card.tilt * lift}deg)`,
              boxShadow: `0 ${40 * lift}px ${90 * lift}px rgba(0,0,0,${0.6 * lift}), 0 0 0 ${2 * lift}px rgba(5,174,255,${0.45 * lift})`,
            }}
          >
            <Img src={still} style={{ position: 'absolute', left: -card.x * k, top: -card.y * k, width: SRC_W * k, height: SRC_H * k }} />
          </div>
        );
      })}
    </AbsoluteFill>
  );
};

// The words of the narration on screen, so the video works muted: each line fades in and rises a little.
const Title = ({ shot, total }: { shot: Shot; total: number }) => {
  const frame = useCurrentFrame();
  const out = interpolate(frame, [total - 12, total], [1, 0], clamp);
  return (
    <AbsoluteFill style={{ alignItems: 'center', paddingTop: 120, opacity: out }}>
      {shot.title.map((line, li) => {
        const p = interpolate(frame, [4 + li * 8, 26 + li * 8], [0, 1], { ...clamp, easing: Easing.out(Easing.cubic) });
        return (
          <div
            key={li}
            style={{
              display: 'flex',
              gap: 22,
              fontSize: 84,
              fontWeight: 900,
              lineHeight: 1.1,
              letterSpacing: -1.5,
              opacity: p,
              transform: `translateY(${(1 - p) * 24}px)`,
            }}
          >
            {line.text.split(' ').map((word, wi) => (
              <span key={wi} style={{ color: line.accent?.includes(word) ? BRAND : 'white' }}>
                {word}
              </span>
            ))}
          </div>
        );
      })}
      {shot.subtitle ? (
        <div
          style={{
            marginTop: 18,
            fontSize: 46,
            fontWeight: 500,
            color: 'rgba(255,255,255,0.8)',
            opacity: interpolate(frame, [26, 40], [0, 1], clamp),
            transform: `translateY(${interpolate(frame, [26, 40], [20, 0], clamp)}px)`,
          }}
        >
          {shot.subtitle}
        </div>
      ) : null}
    </AbsoluteFill>
  );
};

const EndCard = ({ tl }: { tl: Timeline }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const icon = spring({ frame: frame - 24, fps, config: { damping: 11, stiffness: 120 } });
  const name = spring({ frame: frame - 36, fps, config: { damping: 16 } });
  const glow = 0.45 + Math.sin(frame / 20) * 0.1;
  const [first, ...rest] = tl.endCard.name;
  return (
    <AbsoluteFill style={{ alignItems: 'center', justifyContent: 'center', gap: 56 }}>
      <Img
        src={staticFile('icon.png')}
        style={{
          width: 300,
          height: 300,
          borderRadius: 72,
          boxShadow: `0 30px 100px rgba(5,174,255,${glow * icon})`,
          transform: `scale(${icon * (1 + Math.sin(frame / 30) * 0.015)}) rotate(${(1 - icon) * -20}deg)`,
        }}
      />
      <div style={{ fontSize: 120, fontWeight: 900, color: 'white', letterSpacing: -3, opacity: name, transform: `translateY(${(1 - name) * 40}px)` }}>
        {first} <span style={{ color: BRAND }}>{rest.join(' ')}</span>
      </div>
      <div style={{ display: 'flex', gap: 24 }}>
        {tl.endCard.chips.map((chip, i) => {
          const p = spring({ frame: frame - 60 - i * 10, fps, config: { damping: 14 } });
          return (
            <div
              key={chip}
              style={{
                padding: '22px 36px',
                borderRadius: 999,
                background: 'rgba(255,255,255,0.08)',
                border: '2px solid rgba(255,255,255,0.14)',
                color: 'white',
                fontSize: 40,
                fontWeight: 500,
                display: 'flex',
                alignItems: 'center',
                gap: 14,
                opacity: p,
                transform: `translateY(${(1 - p) * 40}px)`,
              }}
            >
              <span style={{ width: 16, height: 16, borderRadius: 8, background: GREEN, display: 'inline-block' }} />
              {chip}
            </div>
          );
        })}
      </div>
    </AbsoluteFill>
  );
};

// The narration, line by line; the music under it, lower while a line plays, ending on the video's last frame; a
// whoosh at each whip and a pop for each lifted card.
const Sound = ({ tl, voiceSeconds, musicSeconds }: { tl: Timeline; voiceSeconds: number[]; musicSeconds: number }) => {
  const last = voiceSeconds.length - 1;
  const spans = [
    ...tl.shots.map((_, i) => [tl.shotStarts[i] + VOICE_AT, tl.shotStarts[i] + VOICE_AT + f(voiceSeconds[i])]),
    [tl.endAt + END_VOICE_AT, tl.endAt + END_VOICE_AT + f(voiceSeconds[last])],
  ];
  const musicVolume = (frame: number) => {
    const duck = Math.max(
      0,
      ...spans.map(([a, b]) =>
        Math.min(interpolate(frame, [a - f(0.2), a], [0, 1], clamp), interpolate(frame, [b, b + f(0.35)], [1, 0], clamp)),
      ),
    );
    return interpolate(frame, [0, f(0.6)], [0, 1], clamp) * (0.5 - 0.32 * duck);
  };
  return (
    <>
      <Audio src={staticFile('music.wav')} startFrom={Math.max(0, f(musicSeconds) - tl.duration)} volume={musicVolume} />
      {tl.shots.map((_, i) => (
        <Sequence key={i} from={tl.shotStarts[i] + VOICE_AT}>
          <Audio src={staticFile(`voice${i + 1}.wav`)} />
        </Sequence>
      ))}
      <Sequence from={tl.endAt + END_VOICE_AT}>
        <Audio src={staticFile(`voice${last + 1}.wav`)} />
      </Sequence>
      {[...tl.whips, tl.endAt].map((at) => (
        <Sequence key={at} from={at - f(0.3)} durationInFrames={f(0.6)}>
          <Audio src={staticFile('whoosh.wav')} volume={0.5} />
        </Sequence>
      ))}
      {tl.placed.flatMap((p) =>
        p.step.kind === 'freeze'
          ? p.step.cards.map((c, i) => (
              <Sequence key={`${p.start}-${i}`} from={p.start + f(c.delay)} durationInFrames={f(0.3)}>
                <Audio src={staticFile('pop.wav')} volume={0.4} />
              </Sequence>
            ))
          : [],
      )}
    </>
  );
};
