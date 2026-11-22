import { AbsoluteFill, Composition, Img, staticFile } from 'remotion';
import { loadFont } from '@remotion/google-fonts/Roboto';
import { BACKDROP, BEZEL_GRADIENT, BRAND } from './theme';

// The YouTube thumbnails of a locale's store video, rendered by thumbnail.sh: the end card's name, a headline and two
// chips beside two of the recording's stills in the video's phone frame, over the video's background. Thumbnail is
// 1280x720; ThumbnailVertical is 1080x1920, for the Shorts feed; FeatureGraphic is Play's 1024x500 feature graphic,
// which is also the cover of the listing's video. thumbnail-index.ts registers them.
const { fontFamily } = loadFont('normal', { weights: ['700', '900'], subsets: ['latin'] });

export type ThumbProps = {
  name: string[];
  headline: string[];
  accent: string[];
  chips: string[];
  back: string;
  front: string;
};

const Phone = ({ src, left, top, width, rotate }: { src: string; left: number; top: number; width: number; rotate: number }) => {
  const bezel = 9;
  const height = Math.round((width * 2400) / 1080);
  return (
    <div
      style={{
        position: 'absolute',
        left,
        top,
        padding: bezel,
        borderRadius: 44,
        background: BEZEL_GRADIENT,
        boxShadow: '0 40px 90px rgba(0,0,0,0.6), 0 0 0 2px rgba(255,255,255,0.06) inset',
        transform: `rotate(${rotate}deg)`,
      }}
    >
      <Img src={staticFile(src)} style={{ width, height, borderRadius: 36, display: 'block' }} />
    </div>
  );
};

// The app's name as the end card draws it: the first word in white, the rest in the brand blue.
const Name = ({ name }: { name: string[] }) => (
  <>
    {name[0]} <span style={{ color: BRAND }}>{name.slice(1).join(' ')}</span>
  </>
);

const Headline = ({ lines, accent }: { lines: string[]; accent: string[] }) => (
  <>
    {lines.map((line) => (
      <div key={line}>
        {line.split(' ').map((word, i) => (
          <span key={i} style={{ color: accent.includes(word) ? BRAND : 'white' }}>
            {i > 0 ? ' ' : ''}
            {word}
          </span>
        ))}
      </div>
    ))}
  </>
);

const Chips = ({ chips, fontSize, padding }: { chips: string[]; fontSize: number; padding: string }) => (
  <>
    {chips.map((chip) => (
      <div
        key={chip}
        style={{
          fontSize,
          fontWeight: 700,
          color: 'white',
          padding,
          borderRadius: 999,
          background: 'rgba(255,255,255,0.10)',
          border: '2px solid rgba(255,255,255,0.18)',
        }}
      >
        {chip}
      </div>
    ))}
  </>
);

export const Thumbnail = ({ name, headline, accent, chips, back, front }: ThumbProps) => (
  <AbsoluteFill
    style={{
      fontFamily,
      overflow: 'hidden',
      background: [
        'radial-gradient(700px 700px at 25% 20%, rgba(5,174,255,0.32), transparent 70%)',
        'radial-gradient(600px 600px at 80% 90%, rgba(74,222,128,0.18), transparent 70%)',
        BACKDROP,
      ].join(', '),
    }}
  >
    <div style={{ position: 'absolute', left: 72, top: 92, width: 640 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 28 }}>
        <Img
          src={staticFile('icon.png')}
          style={{ width: 128, height: 128, borderRadius: 30, boxShadow: '0 20px 60px rgba(5,174,255,0.45)' }}
        />
        <div style={{ fontSize: 88, fontWeight: 900, color: 'white', letterSpacing: -2, lineHeight: 1 }}>
          <Name name={name} />
        </div>
      </div>
      <div style={{ marginTop: 56, fontSize: 64, fontWeight: 900, color: 'white', lineHeight: 1.08, letterSpacing: -1 }}>
        <Headline lines={headline} accent={accent} />
      </div>
      <div style={{ marginTop: 44, display: 'flex', gap: 16, flexWrap: 'wrap' }}>
        <Chips chips={chips} fontSize={30} padding="12px 26px" />
      </div>
    </div>
    <Phone src={back} left={760} top={70} width={250} rotate={-7} />
    <Phone src={front} left={960} top={150} width={250} rotate={6} />
  </AbsoluteFill>
);

// The Shorts feed shows a vertical video's thumbnail at 9:16, so the vertical one stacks the same pieces: name and
// words on top, where the feed's own title and buttons don't cover them, and the phones below.
export const ThumbnailVertical = ({ name, headline, accent, chips, back, front }: ThumbProps) => (
  <AbsoluteFill
    style={{
      fontFamily,
      overflow: 'hidden',
      background: [
        'radial-gradient(1000px 1000px at 30% 15%, rgba(5,174,255,0.32), transparent 70%)',
        'radial-gradient(900px 900px at 75% 85%, rgba(74,222,128,0.18), transparent 70%)',
        BACKDROP,
      ].join(', '),
    }}
  >
    <div style={{ position: 'absolute', top: 150, left: 0, right: 0, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 36 }}>
        <Img
          src={staticFile('icon.png')}
          style={{ width: 180, height: 180, borderRadius: 42, boxShadow: '0 24px 80px rgba(5,174,255,0.45)' }}
        />
        <div style={{ fontSize: 124, fontWeight: 900, color: 'white', letterSpacing: -3, lineHeight: 1 }}>
          <Name name={name} />
        </div>
      </div>
      <div
        style={{ marginTop: 90, padding: '0 60px', fontSize: 104, fontWeight: 900, color: 'white', lineHeight: 1.06, letterSpacing: -2, textAlign: 'center' }}
      >
        <Headline lines={headline} accent={accent} />
      </div>
      <div style={{ marginTop: 60, padding: '0 60px', display: 'flex', gap: 22, flexWrap: 'wrap', justifyContent: 'center' }}>
        <Chips chips={chips} fontSize={42} padding="16px 34px" />
      </div>
    </div>
    <Phone src={back} left={110} top={930} width={400} rotate={-7} />
    <Phone src={front} left={560} top={1010} width={400} rotate={6} />
  </AbsoluteFill>
);

// Play draws its play button over the middle of the feature graphic when the listing has a video, and crops its edges
// in some places, so the words keep to the left, clear of the edges, and the phones to the right, where the front one
// may run off the bottom edge as the thumbnails' do: a crop there only cuts more of the phone.
export const FeatureGraphic = ({ name, headline, accent, chips, back, front }: ThumbProps) => (
  <AbsoluteFill
    style={{
      fontFamily,
      overflow: 'hidden',
      background: [
        'radial-gradient(560px 560px at 22% 25%, rgba(5,174,255,0.32), transparent 70%)',
        'radial-gradient(480px 480px at 85% 95%, rgba(74,222,128,0.18), transparent 70%)',
        BACKDROP,
      ].join(', '),
    }}
  >
    <div style={{ position: 'absolute', left: 56, top: 70, width: 480 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 20 }}>
        <Img
          src={staticFile('icon.png')}
          style={{ width: 88, height: 88, borderRadius: 21, boxShadow: '0 14px 44px rgba(5,174,255,0.45)' }}
        />
        <div style={{ fontSize: 60, fontWeight: 900, color: 'white', letterSpacing: -1.5, lineHeight: 1 }}>
          <Name name={name} />
        </div>
      </div>
      <div style={{ marginTop: 40, fontSize: 46, fontWeight: 900, color: 'white', lineHeight: 1.08, letterSpacing: -0.5 }}>
        <Headline lines={headline} accent={accent} />
      </div>
      <div style={{ marginTop: 30, display: 'flex', gap: 12, flexWrap: 'wrap' }}>
        <Chips chips={chips} fontSize={21} padding="9px 18px" />
      </div>
    </div>
    <Phone src={back} left={610} top={64} width={170} rotate={-7} />
    <Phone src={front} left={790} top={104} width={170} rotate={6} />
  </AbsoluteFill>
);

const defaults: ThumbProps = { name: ['Bora', 'Rachar'], headline: [], accent: [], chips: [], back: 'bal_35.png', front: 'pix_101.png' };

export const ThumbnailRoot = () => (
  <>
    <Composition id="Thumbnail" component={Thumbnail} width={1280} height={720} fps={30} durationInFrames={1} defaultProps={defaults} />
    <Composition
      id="ThumbnailVertical"
      component={ThumbnailVertical}
      width={1080}
      height={1920}
      fps={30}
      durationInFrames={1}
      defaultProps={defaults}
    />
    <Composition id="FeatureGraphic" component={FeatureGraphic} width={1024} height={500} fps={30} durationInFrames={1} defaultProps={defaults} />
  </>
);
