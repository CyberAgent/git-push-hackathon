import React, { Component } from "react";
import File from "./File";

class Editor extends Component {
  constructor() {
    super();

    this.state = {
      description: "",
      public: true,
      files: [{ index: 0, file: "", content: "" }],
      isSubmit: false,
      isSetGist: false
    };
  }

  componentDidMount() {
    this.setGist();
  }

  componentWillReceiveProps() {
    if (!this.state.isSetGist) {
      this.setGist();
    }
  }

  setGist() {
    const {
      gist,
      match: {
        params: { id }
      }
    } = this.props;
    const currentGist = gist[id];

    if (currentGist) {
      let files = [];
      let index = 0;
      for (let name in currentGist.files) {
        const file = {
          file: name,
          content: currentGist.files[name].content,
          index: index
        };
        files.push(file);
        index++;
      }

      this.setState({ ...currentGist, files: files, isSetGist: true });
    }
  }

  handleChange(keyValue) {
    this.setState(keyValue);
  }

  handleFileChange(keyValue, index) {
    const { files } = this.state;
    const value = { ...this.state.files[index], ...keyValue };
    let newFiles = files.concat();
    newFiles[index] = value;

    this.setState({ files: newFiles });
  }

  addFile() {
    const { files } = this.state;
    const newFile = { index: files.length, file: "", content: "" };
    const newFiles = this.state.files.concat(newFile);
    this.setState({ files: newFiles });
  }

  handleSubmit() {
    if (!this.state.isSubmit) {
      const { type } = this.props;
      const {
        actions: { createGist }
      } = this.props;
      if (type == "create") {
        createGist({ data: this.state });
      }
      this.setState({ isSubmit: true });
    }
  }

  fileEditors() {
    const fileEditorList = this.state.files.map(f => {
      return (
        <li key={f.index}>
          <File
            {...f}
            onChange={keyValue => this.handleFileChange(keyValue, f.index)}
          />
        </li>
      );
    });
    return fileEditorList;
  }

  render() {
    const { description } = this.state;
    const { type } = this.props;
    const buttonMessage = type == "create" ? "Create" : "Edit";
    return (
      <div>
        <input
          type="text"
          value={description}
          placeholder="description"
          onChange={e => this.handleChange({ description: e.target.value })}
        />
        <ul>{this.fileEditors()}</ul>
        <button onClick={() => this.addFile()}>Add File</button>
        <button onClick={() => this.handleSubmit()}>{buttonMessage}</button>
      </div>
    );
  }
}

export default Editor;
