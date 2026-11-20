import { Composition } from 'remotion';
import { FPS, LOCALES, StoreVideo, StoreVideoProps, timelineOf } from './StoreVideo';

// One composition; generate.sh passes the locale and the lengths of its narration and music as props.
const defaults: StoreVideoProps = { locale: 'pt-BR', voiceSeconds: [2.6, 3, 3.3, 4.7, 4.3, 2.9], musicSeconds: 32 };

export const Root = () => (
  <Composition
    id="StoreVideo"
    component={StoreVideo}
    fps={FPS}
    width={1080}
    height={1920}
    defaultProps={defaults}
    durationInFrames={timelineOf(LOCALES[defaults.locale]).duration}
    calculateMetadata={({ props }) => ({ durationInFrames: timelineOf(LOCALES[props.locale]).duration })}
  />
);
