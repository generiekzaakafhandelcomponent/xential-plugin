import {Component, OnInit} from '@angular/core';
import {IconService} from 'carbon-components-angular';
import {ListChecked16} from '@carbon/icons';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  standalone: false
})
export class AppComponent implements OnInit {
  constructor(private readonly iconService: IconService) {
    // @valtimo/process-management's BPMN builder toolbar uses cdsIcon="list--checked"
    // for the Validate button but never registers it itself, which throws
    // IconNameNotFoundError during change detection and breaks rendering of the
    // rest of the BPMN editor (including the process-link panel).
    this.iconService.registerAll([ListChecked16]);
  }

  ngOnInit() {
  }
}
