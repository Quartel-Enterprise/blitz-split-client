import { Composition } from 'remotion';
import ptBR from '../../locales/pt-BR.json';
import { FPS, Locale, StoreVideo, StoreVideoProps, timelineOf } from './StoreVideo';

// One composition; generate.sh passes the locale's shots and the lengths of its narration and music as props
// (build/store-video/<locale>/props.json). pt-BR's are the defaults, for Remotion's studio.
const defaults: StoreVideoProps = {
  video: ptBR as Locale,
  voiceSeconds: [2.64, 3.04, 3.28, 4.72, 4.32, 2.88],
  musicSeconds: 32.04,
};

export const Root = () => (
  <Composition
    id="StoreVideo"
    component={StoreVideo}
    fps={FPS}
    width={1080}
    height={1920}
    defaultProps={defaults}
    durationInFrames={timelineOf(defaults.video).duration}
    calculateMetadata={({ props }) => ({ durationInFrames: timelineOf(props.video).duration })}
  />
);
