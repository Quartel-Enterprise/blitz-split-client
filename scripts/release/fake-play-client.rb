# A stand-in for supply's Google Play client, for play-screenshots-lane.test.mjs: with it loaded (RUBYOPT=-r<this
# file>), the lanes of fastlane/Fastfile run for real and no request leaves the machine.
#
#   FAKE_PLAY_HELD          a JSON file of what Google Play holds: {"<locale>":{"<shelf>":["<sha256>",…]}}
#   FAKE_PLAY_LOG           the file every call that would reach Google Play is written to, one per line
#   FAKE_PLAY_COMMIT_ERROR  when set, what Google Play answers the commit of an edit with
require "json"
require "ostruct"

class FakePlayClient
  Image = Struct.new(:id, :sha256)

  def initialize
    @held = JSON.parse(File.read(ENV.fetch("FAKE_PLAY_HELD")))
    @open = false
  end

  def begin_edit(package_name: nil)
    raise "an edit is already open" if @open

    @open = true
    log("begin_edit")
  end

  def abort_current_edit
    @open = false
    log("abort_edit")
  end

  def fetch_images(image_type:, language:)
    raise "no edit is open" unless @open

    (@held.dig(language, image_type) || []).each_with_index.map { |sha, index| Image.new("#{language}-#{image_type}-#{index}", sha) }
  end

  def listing_for_language(language)
    OpenStruct.new(language: language)
  end

  # The production track with one release, which supply looks up before it uploads a listing's images
  def tracks(*)
    [OpenStruct.new(track: "production", releases: [OpenStruct.new(version_codes: [1], status: "completed")])]
  end

  def clear_screenshot(image_type:, language:, image_id:)
    log("delete #{language}/#{image_type}/#{image_id}")
  end

  def upload_image(image_path:, image_type:, language:)
    log("upload #{language}/#{image_type}/#{File.basename(image_path)}")
  end

  def validate_current_edit!
    log("validate")
  end

  # As supply's client does: the error is fastlane's, worded "Google Api Error: <what Google answered>"
  def commit_current_edit!
    log("commit")
    refusal = ENV["FAKE_PLAY_COMMIT_ERROR"].to_s
    FastlaneCore::UI.user_error!(refusal) unless refusal.empty?
    @open = false
  end

  private

  def log(line)
    File.open(ENV.fetch("FAKE_PLAY_LOG"), "a") { |file| file.puts(line) }
  end
end

# supply is loaded long after this file, so the stand-in takes the client's place when its class is done being
# defined. Module#name is called on the class itself: some of fastlane's classes define a `name` of their own.
TracePoint.new(:end) do |trace|
  owner = trace.self
  next unless owner.is_a?(Class) && Module.instance_method(:name).bind_call(owner) == "Supply::Client"
  next if owner.singleton_class.method_defined?(:faked?)

  owner.define_singleton_method(:faked?) { true }
  owner.define_singleton_method(:make_from_config) { |params: nil| FakePlayClient.new }
end.enable
